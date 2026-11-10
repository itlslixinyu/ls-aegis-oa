package com.ls.aegis.biz.psi.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real production mapper SQL, independent H2 transactions and two worker threads. No live datasource. */
class StockMutationConcurrencyTest {
    final ExecutorService pool=Executors.newFixedThreadPool(2);
    JdbcTemplate db;
    TransactionTemplate tx;
    StockMutationLock lock;
    StockMutationLockMapper anchors;
    StockMutationLockMapper realAnchors;
    StockBatchMapper batches;
    FifoStockHelper fifo;
    DocNoGenerator numbers;
    final LocalDateTime now=LocalDateTime.of(2026,11,6,0,0);
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        String metadata="id bigint primary key,tenant_id bigint,deleted bigint default 0,create_user bigint,create_time timestamp,update_user bigint,update_time timestamp";
        db.execute("create table psi_stock_mutation_lock(tenant_id bigint,warehouse_id bigint,goods_id bigint,primary key(tenant_id,warehouse_id,goods_id))");
        db.execute("create table psi_stock_batch("+metadata+",warehouse_id bigint,goods_id bigint,batch_no varchar(100),source_type varchar(32),source_id bigint,source_no varchar(100),in_date timestamp,origin_in_date timestamp,total_quantity decimal(18,4),remain_quantity decimal(18,4),untax_price decimal(18,4),total_amount decimal(18,2),remain_amount decimal(18,2))");
        db.execute("create table psi_stock_flow("+metadata+",dept_id bigint,warehouse_id bigint,goods_id bigint,flow_type varchar(32),before_qty decimal(18,4),change_qty decimal(18,4),after_qty decimal(18,4),before_amount decimal(18,2),change_amount decimal(18,2),after_amount decimal(18,2),source_type varchar(32),source_id bigint,source_no varchar(100),confirm_user bigint,confirm_time timestamp)");
        var c=new MybatisConfiguration();c.setMapUnderscoreToCamelCase(true);c.setDatabaseId("mysql");
        var global=new GlobalConfig();global.setDbConfig(new GlobalConfig.DbConfig().setLogicDeleteField("deleted").setLogicNotDeleteValue("0").setLogicDeleteValue("id"));GlobalConfigUtils.setGlobalConfig(c,global);
        c.setEnvironment(new Environment("test",new SpringManagedTransactionFactory(),ds));
        c.addMapper(StockBatchMapper.class);c.addMapper(StockFlowMapper.class);
        try(InputStream in=Files.newInputStream(Path.of("src/main/resources/mapper/StockMutationLockMapper.xml"))){new XMLMapperBuilder(in,c,"anchors",c.getSqlFragments()).parse();}
        var session=new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(c));
        realAnchors=session.getMapper(StockMutationLockMapper.class);anchors=spy(realAnchors);lock=new StockMutationLock(anchors);
        batches=session.getMapper(StockBatchMapper.class);
        numbers=mock(DocNoGenerator.class);var sequence=new AtomicInteger();when(numbers.next(anyLong(),anyString())).thenAnswer(inv->"TEST-BATCH-"+sequence.incrementAndGet());
        fifo=new FifoStockHelper(batches,session.getMapper(StockFlowMapper.class),numbers,lock);
    }
    @AfterEach void close() throws Exception {
        pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));
        UserContextHolder.clearContext();if(db!=null) db.execute("SHUTDOWN");
    }
    void transaction(Runnable body) {
        var user=new UserContext();user.setId(100L);user.setTenantId(0L);user.setDeptId(10L);UserContextHolder.setContext(user,false);
        try {tx.executeWithoutResult(status->body.run());} finally {UserContextHolder.clearContext();}
    }
    void inbound(long warehouse,long goods,int quantity) {fifo.inbound(0L,warehouse,goods,BigDecimal.valueOf(quantity),BigDecimal.TEN,BigDecimal.valueOf(quantity*10L),now,BizSourceTypeEnum.STOCK_IN,1L,"TEST-IN");}
    void await(CountDownLatch latch) {try {assertTrue(latch.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
    BigDecimal sum(String col){return db.queryForObject("select coalesce(sum("+col+"),0) from psi_stock_batch",BigDecimal.class);}
    void assertChain(int firstBefore,int lastAfter) {
        var flows=db.queryForList("select before_qty,after_qty,before_amount,after_amount from psi_stock_flow order by id");
        BigDecimal quantity=BigDecimal.valueOf(firstBefore),amount=quantity.multiply(BigDecimal.TEN);
        for(var flow:flows){assertEquals(0,quantity.compareTo((BigDecimal)flow.get("BEFORE_QTY")));assertEquals(0,amount.compareTo((BigDecimal)flow.get("BEFORE_AMOUNT")));quantity=(BigDecimal)flow.get("AFTER_QTY");amount=(BigDecimal)flow.get("AFTER_AMOUNT");}
        assertEquals(0,quantity.compareTo(BigDecimal.valueOf(lastAfter)));assertEquals(0,quantity.compareTo(sum("remain_quantity")));assertEquals(0,amount.compareTo(sum("remain_amount")));
    }
    @Test void twoConcurrentFirstInboundsSerializeEvenWhenThereIsNoBatchToLock() throws Exception {
        var firstReached=new CountDownLatch(1);var releaseFirst=new CountDownLatch(1);var next=new AtomicInteger();
        when(numbers.next(anyLong(),anyString())).thenAnswer(inv->{int n=next.incrementAndGet();if(n==1){firstReached.countDown();await(releaseFirst);}return "TEST-EMPTY-"+n;});
        Future<?> first=pool.submit(()->transaction(()->inbound(1,11,5)));await(firstReached);
        var secondStarted=new CountDownLatch(1);Future<?> second=pool.submit(()->transaction(()->{
            assertEquals(0,batches.sumRemainQuantity(0L,1L,11L).compareTo(BigDecimal.ZERO));secondStarted.countDown();inbound(1,11,7);
        }));await(secondStarted);releaseFirst.countDown();first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);
        assertChain(0,12);assertEquals(1,db.queryForObject("select count(*) from psi_stock_mutation_lock",Integer.class));
    }
    @Test void secondOutboundWaitsBeforeReadingBeforeSnapshotRatherThanOnlyAtFifoBatchLock() throws Exception {
        transaction(()->inbound(1,11,10));db.update("delete from psi_stock_flow");
        var firstWritten=new CountDownLatch(1);var releaseFirst=new CountDownLatch(1);
        Future<?> first=pool.submit(()->transaction(()->{fifo.outbound(0L,1L,11L,BigDecimal.valueOf(3),BizSourceTypeEnum.STOCK_OUT,2L,"TEST-OUT-1");firstWritten.countDown();await(releaseFirst);}));await(firstWritten);
        var secondRead=new CountDownLatch(1);Future<?> second=pool.submit(()->transaction(()->{
            assertEquals(0,batches.sumRemainQuantity(0L,1L,11L).compareTo(BigDecimal.TEN));secondRead.countDown();fifo.outbound(0L,1L,11L,BigDecimal.valueOf(4),BizSourceTypeEnum.STOCK_OUT,3L,"TEST-OUT-2");
        }));await(secondRead);releaseFirst.countDown();first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);assertChain(10,3);
    }
    @Test void oppositeInputOrdersAcquireWholeFootprintInSameWarehouseGoodsOrder() throws Exception {
        var a=new StockMutationLock.Key(0L,1L,11L);var b=new StockMutationLock.Key(0L,2L,10L);var c=new StockMutationLock.Key(0L,1L,12L);
        var observed=new ConcurrentHashMap<Long,List<StockMutationLock.Key>>();
        doAnswer(inv->{observed.computeIfAbsent(Thread.currentThread().getId(),id->new ArrayList<>()).add(new StockMutationLock.Key(inv.getArgument(0),inv.getArgument(1),inv.getArgument(2)));return realAnchors.ensureAnchor(inv.getArgument(0),inv.getArgument(1),inv.getArgument(2));}).when(anchors).ensureAnchor(anyLong(),anyLong(),anyLong());
        var start=new CountDownLatch(1);Future<?> first=pool.submit(()->{await(start);transaction(()->lock.lockAll(List.of(b,c,a,a)));});Future<?> second=pool.submit(()->{await(start);transaction(()->lock.lockAll(List.of(a,c,b)));});start.countDown();
        first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);
        assertEquals(2,observed.size());for(var order:observed.values())assertEquals(List.of(a,c,b),order);
        assertEquals(3,db.queryForObject("select count(*) from psi_stock_mutation_lock",Integer.class));
    }
    @Test void rolledBackFirstInboundLeavesNoAnchorBatchOrFlowAndRetryCanStartFromZero() {
        assertThrows(IllegalStateException.class,()->transaction(()->{inbound(1,11,5);throw new IllegalStateException("rollback");}));
        assertEquals(0,db.queryForObject("select count(*) from psi_stock_mutation_lock",Integer.class));assertEquals(0,sum("remain_quantity").signum());
        transaction(()->inbound(1,11,2));assertChain(0,2);
    }
    @Test void dimensionWithoutTransactionFailsClosedAndTenantDimensionIsIsolated() {
        assertThrows(RuntimeException.class,()->lock.lock(0L,1L,11L));
        transaction(()->lock.lockAll(List.of(new StockMutationLock.Key(0L,1L,11L),new StockMutationLock.Key(7L,1L,11L))));
        assertEquals(2,db.queryForObject("select count(*) from psi_stock_mutation_lock",Integer.class));
    }
}
