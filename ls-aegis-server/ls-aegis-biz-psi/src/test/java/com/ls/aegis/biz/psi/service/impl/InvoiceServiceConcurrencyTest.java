package com.ls.aegis.biz.psi.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.support.*;
import com.ls.aegis.common.context.*;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.continew.starter.core.exception.BusinessException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual invoice service decisions, production global-ledger SQL and independent Spring/H2 transactions. */
class InvoiceServiceConcurrencyTest {
    JdbcTemplate db;TransactionTemplate tx;InvoiceLedgerMapper ledger;InvoiceOutServiceImpl out;InvoiceInServiceImpl in;
    final InvoiceOutMapper outs=mock(InvoiceOutMapper.class);final InvoiceInMapper ins=mock(InvoiceInMapper.class);
    final StockOutMapper stockOuts=mock(StockOutMapper.class);final StockInMapper stockIns=mock(StockInMapper.class);
    volatile CyclicBarrier invoiceGate;
    @BeforeAll static void metadata(){for(Class<?> type:List.of(InvoiceOutDO.class,InvoiceInDO.class,InvoiceOutLinkDO.class,InvoiceInLinkDO.class,InvoiceOutItemDO.class,InvoiceInItemDO.class)){
        var helper=new MapperBuilderAssistant(new MybatisConfiguration(),"invoice-service-test");helper.setCurrentNamespace(type.getName());TableInfoHelper.initTableInfo(helper,type);
    }}
    @BeforeEach @SuppressWarnings("unchecked") void setup()throws Exception{
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        for(String side:List.of("out","in")){
            db.execute("create table psi_stock_"+side+"(id bigint primary key,tenant_id bigint,unit_id bigint,doc_status varchar(20),tax_inclusive_amount decimal(18,2),invoice_status varchar(20),deleted bigint default 0)");
            db.execute("create table psi_invoice_"+side+"(id bigint primary key,tenant_id bigint,dept_id bigint,doc_no varchar(64),doc_status varchar(20),invoice_no varchar(64),invoice_type varchar(20),invoice_date timestamp,unit_id bigint,buyer_tax_no varchar(64),seller_tax_no varchar(64),tax_inclusive_amount decimal(18,2),untax_amount decimal(18,2),tax_amount decimal(18,2),red_of_id bigint,idempotent_key varchar(100),create_user bigint default 99,confirm_user bigint,confirm_time timestamp,deleted bigint default 0)");
            db.execute("create table psi_invoice_"+side+"_link(id bigint primary key,tenant_id bigint,invoice_id bigint,stock_"+side+"_id bigint,amount decimal(18,2),deleted bigint default 0)");
            db.execute("create table psi_invoice_"+side+"_item(id bigint primary key,tenant_id bigint,invoice_id bigint,goods_name varchar(100),quantity decimal(18,4),tax_inclusive_amount decimal(18,2),untax_amount decimal(18,2),tax_amount decimal(18,2),tax_rate decimal(8,2),sort int,deleted bigint default 0)");
            db.update("insert into psi_stock_"+side+"(id,tenant_id,unit_id,doc_status,tax_inclusive_amount,invoice_status) values(100,0,10,'CONFIRMED',100,'UNINVOICED')");
            for(long id:List.of(1L,2L)){
                db.update("insert into psi_invoice_"+side+"(id,tenant_id,dept_id,doc_no,doc_status,invoice_no,invoice_type,invoice_date,unit_id,buyer_tax_no,seller_tax_no,tax_inclusive_amount,untax_amount,tax_amount) values(?,0,? ,?,'DRAFT',?,'NORMAL',CURRENT_TIMESTAMP,10,'','',100,100,0)",id,id==1?12:999,"TEST-DOC-"+id,"TEST-INVOICE-"+id);
                db.update("insert into psi_invoice_"+side+"_link(id,tenant_id,invoice_id,stock_"+side+"_id,amount) values(?,0,?,100,100)",id,id);
            }
        }
        var c=new MybatisConfiguration();c.setMapUnderscoreToCamelCase(true);c.setEnvironment(new Environment("test",new SpringManagedTransactionFactory(),ds));c.addMapper(InvoiceLedgerMapper.class);ledger=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(c)).getMapper(InvoiceLedgerMapper.class);
        var status=new InvoiceStatusSupport(ledger,stockOuts,stockIns);var idem=mock(IdempotentHelper.class);when(idem.execute(anyLong(),anyString(),anyString(),any())).thenAnswer(inv->((Supplier<String>)inv.getArgument(3)).get());
        var numbers=mock(DocNoGenerator.class);var sequence=new AtomicLong(1000);when(numbers.next(anyLong(),anyString())).thenAnswer(inv->"TEST-RED-"+sequence.incrementAndGet());
        when(outs.selectByIdForUpdate(anyLong())).thenAnswer(inv->invoice("out",inv.getArgument(0),InvoiceOutDO.class));when(ins.selectByIdForUpdate(anyLong())).thenAnswer(inv->invoice("in",inv.getArgument(0),InvoiceInDO.class));
        when(stockOuts.selectByIdForUpdate(anyLong())).thenAnswer(inv->one("psi_stock_out",inv.getArgument(0),StockOutDO.class,true));when(stockIns.selectByIdForUpdate(anyLong())).thenAnswer(inv->one("psi_stock_in",inv.getArgument(0),StockInDO.class,true));
        when(stockOuts.updateById(any(StockOutDO.class))).thenAnswer(inv->{StockOutDO row=inv.getArgument(0);return db.update("update psi_stock_out set invoice_status=? where id=?",row.getInvoiceStatus().name(),row.getId());});
        when(stockIns.updateById(any(StockInDO.class))).thenAnswer(inv->{StockInDO row=inv.getArgument(0);return db.update("update psi_stock_in set invoice_status=? where id=?",row.getInvoiceStatus().name(),row.getId());});
        when(outs.updateById(any(InvoiceOutDO.class))).thenAnswer(inv->{InvoiceOutDO row=inv.getArgument(0);return db.update("update psi_invoice_out set doc_status=? where id=?",row.getDocStatus().name(),row.getId());});
        when(ins.updateById(any(InvoiceInDO.class))).thenAnswer(inv->{InvoiceInDO row=inv.getArgument(0);return db.update("update psi_invoice_in set doc_status=? where id=?",row.getDocStatus().name(),row.getId());});
        when(outs.insert(any(InvoiceOutDO.class))).thenAnswer(inv->{InvoiceOutDO row=inv.getArgument(0);row.setId(sequence.incrementAndGet());return insertRed("out",row);});when(ins.insert(any(InvoiceInDO.class))).thenAnswer(inv->{InvoiceInDO row=inv.getArgument(0);row.setId(sequence.incrementAndGet());return insertRed("in",row);});
        var outItems=mock(InvoiceOutItemMapper.class);var inItems=mock(InvoiceInItemMapper.class);var outLinks=mock(InvoiceOutLinkMapper.class);var inLinks=mock(InvoiceInLinkMapper.class);
        when(outItems.selectList(any())).thenReturn(List.of());when(inItems.selectList(any())).thenReturn(List.of());
        when(outLinks.selectList(any())).thenAnswer(inv->links("out",inv.getArgument(0),InvoiceOutLinkDO.class));when(inLinks.selectList(any())).thenAnswer(inv->links("in",inv.getArgument(0),InvoiceInLinkDO.class));
        var todos=mock(PsiTodoCloseSupport.class);
        out=new InvoiceOutServiceImpl(outs,outItems,outLinks,stockOuts,mock(UnitMapper.class),numbers,idem,status,todos,mock(com.ls.aegis.biz.psi.support.PsiAttachmentAccessGuard.class));in=new InvoiceInServiceImpl(ins,inItems,inLinks,stockIns,mock(UnitMapper.class),numbers,idem,status,todos,mock(com.ls.aegis.biz.psi.support.PsiAttachmentAccessGuard.class));
        identity();
    }
    @AfterEach void clear(){UserContextHolder.clearContext();db.execute("SHUTDOWN");}
    void identity(){var actor=new UserContext();actor.setId(99L);actor.setTenantId(0L);actor.setDeptId(12L);UserContextHolder.setContext(actor,false);}
    <T>T one(String table,Long id,Class<T> type,boolean lock){var rows=db.query("select * from "+table+" where id=? and deleted=0"+(lock?" for update":""),new BeanPropertyRowMapper<>(type),id);return rows.isEmpty()?null:rows.get(0);}
    <T>T invoice(String side,Long id,Class<T> type)throws Exception{T row=one("psi_invoice_"+side,id,type,true);CyclicBarrier gate=invoiceGate;if(gate!=null && id<=2)gate.await(5,TimeUnit.SECONDS);return row;}
    <T>List<T> links(String side,AbstractWrapper<?,?,?> wrapper,Class<T> type){wrapper.getSqlSegment();Long id=((Number)wrapper.getParamNameValuePairs().values().iterator().next()).longValue();return db.query("select * from psi_invoice_"+side+"_link where invoice_id=? and deleted=0",new BeanPropertyRowMapper<>(type),id);}
    int insertRed(String side,Object row){var r=cn.hutool.core.bean.BeanUtil.copyProperties(row,com.ls.aegis.biz.psi.model.resp.InvoiceResp.class);Long redOf=(Long)cn.hutool.core.bean.BeanUtil.getProperty(row,"redOfId");return db.update("insert into psi_invoice_"+side+"(id,tenant_id,dept_id,doc_no,doc_status,invoice_no,invoice_type,invoice_date,unit_id,buyer_tax_no,seller_tax_no,tax_inclusive_amount,untax_amount,tax_amount,red_of_id) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",r.getId(),cn.hutool.core.bean.BeanUtil.getProperty(row,"tenantId"),12,r.getDocNo(),"CONFIRMED",r.getInvoiceNo(),r.getInvoiceType().name(),r.getInvoiceDate(),r.getUnitId(),r.getBuyerTaxNo(),r.getSellerTaxNo(),r.getTaxInclusiveAmount(),r.getUntaxAmount(),r.getTaxAmount(),redOf);}
    String call(boolean outgoing,long id,boolean red){var req=new DocConfirmReq();req.setIdempotentKey(UUID.randomUUID().toString());return outgoing?(red?out.red(id,req):out.confirm(id,req)):(red?in.red(id,req):in.confirm(id,req));}
    BigDecimal linked(boolean outgoing,long tenant){return outgoing?ledger.outLinked(tenant,100L,null):ledger.inLinked(tenant,100L,null);}
    @ParameterizedTest @ValueSource(booleans={true,false}) void twoBlueInvoicesForSameStockCannotBothConsumeItsBalance(boolean outgoing)throws Exception{
        invoiceGate=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);String side=outgoing?"out":"in";
        try{List<Future<Boolean>> jobs=new ArrayList<>();for(long id:List.of(1L,2L))jobs.add(pool.submit(()->{identity();try{return tx.execute(s->{call(outgoing,id,false);return true;});}catch(BusinessException rejected){return false;}finally{UserContextHolder.clearContext();}}));
            int successes=0;for(var job:jobs)if(job.get(15,TimeUnit.SECONDS))successes++;
            assertEquals(1,successes);assertEquals(1,db.queryForObject("select count(*) from psi_invoice_"+side+" where doc_status='CONFIRMED'",Integer.class));assertEquals(0,new BigDecimal("100").compareTo(linked(outgoing,0)));
        }finally{invoiceGate=null;pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void redRetryReturnsSameCounterDocumentAndRedCannotRedAgain(boolean outgoing){
        String side=outgoing?"out":"in";tx.execute(s->call(outgoing,1,false));String first=tx.execute(s->call(outgoing,1,true));String replay=tx.execute(s->call(outgoing,1,true));assertEquals(first,replay);
        assertEquals(1,db.queryForObject("select count(*) from psi_invoice_"+side+" where red_of_id=1",Integer.class));assertEquals(0,linked(outgoing,0).signum());
        Long redId=db.queryForObject("select id from psi_invoice_"+side+" where red_of_id=1",Long.class);assertThrows(BusinessException.class,()->tx.execute(s->call(outgoing,redId,true)));
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void wrongUnitCannotConfirmOrOccupySourceBalance(boolean outgoing){String side=outgoing?"out":"in";db.update("update psi_invoice_"+side+" set unit_id=20 where id=1");assertThrows(BusinessException.class,()->tx.execute(s->call(outgoing,1,false)));assertEquals(0,linked(outgoing,0).signum());}
    @ParameterizedTest @ValueSource(booleans={true,false}) void legacyWrongTenantLinkCannotBeConfirmedThenDisappearFromGlobalCapacity(boolean outgoing){String side=outgoing?"out":"in";db.update("update psi_invoice_"+side+"_link set tenant_id=7 where invoice_id=1");assertThrows(BusinessException.class,()->tx.execute(s->call(outgoing,1,false)));assertEquals("DRAFT",db.queryForObject("select doc_status from psi_invoice_"+side+" where id=1",String.class));}
    @ParameterizedTest @ValueSource(booleans={true,false}) void duplicatePersistedLinksCannotBypassSourceBalance(boolean outgoing){String side=outgoing?"out":"in";db.update("insert into psi_invoice_"+side+"_link(id,tenant_id,invoice_id,stock_"+side+"_id,amount) values(3,0,1,100,1)");assertThrows(BusinessException.class,()->tx.execute(s->call(outgoing,1,false)));assertEquals(0,linked(outgoing,0).signum());}
    @ParameterizedTest @ValueSource(booleans={true,false}) void authorizedSourceTenantControlsCapacityEvenWhenOperatorContextDiffers(boolean outgoing){
        String side=outgoing?"out":"in";db.update("update psi_stock_"+side+" set tenant_id=7");db.update("update psi_invoice_"+side+" set tenant_id=7");db.update("update psi_invoice_"+side+"_link set tenant_id=7");
        db.update("update psi_invoice_"+side+" set tax_inclusive_amount=30,untax_amount=30 where id=1");db.update("update psi_invoice_"+side+"_link set amount=30 where invoice_id=1");
        db.update("update psi_invoice_"+side+" set doc_status='CONFIRMED',tax_inclusive_amount=80,untax_amount=80 where id=2");db.update("update psi_invoice_"+side+"_link set amount=80 where invoice_id=2");
        assertThrows(BusinessException.class,()->tx.execute(s->call(outgoing,1,false)));assertEquals(0,new BigDecimal("80").compareTo(linked(outgoing,7)));
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void simultaneousRedRequestsProduceOnlyOneCounterInvoice(boolean outgoing)throws Exception{
        tx.execute(s->call(outgoing,1,false));var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);String side=outgoing?"out":"in";
        try{List<Future<String>> jobs=new ArrayList<>();for(int i=0;i<2;i++)jobs.add(pool.submit(()->{identity();try{assertTrue(start.await(5,TimeUnit.SECONDS));return tx.execute(s->call(outgoing,1,true));}finally{UserContextHolder.clearContext();}}));start.countDown();assertEquals(jobs.get(0).get(15,TimeUnit.SECONDS),jobs.get(1).get(15,TimeUnit.SECONDS));assertEquals(1,db.queryForObject("select count(*) from psi_invoice_"+side+" where red_of_id=1",Integer.class));}
        finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void counterInsertZeroKeepsBlueConfirmedAndCapacityOccupied(boolean outgoing){
        tx.execute(s->call(outgoing,1,false));
        if(outgoing)doReturn(0).when(outs).insert(any(InvoiceOutDO.class));
        else doReturn(0).when(ins).insert(any(InvoiceInDO.class));
        assertThrows(BusinessException.class,()->tx.execute(s->call(outgoing,1,true)));
        assertOriginalBlueAndCapacity(outgoing);
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void blueUpdateZeroRollsBackAnAlreadyInsertedCounterInvoice(boolean outgoing){
        tx.execute(s->call(outgoing,1,false));
        if(outgoing)doReturn(0).when(outs).updateById(any(InvoiceOutDO.class));
        else doReturn(0).when(ins).updateById(any(InvoiceInDO.class));
        assertThrows(BusinessException.class,()->tx.execute(s->call(outgoing,1,true)));
        if(outgoing)verify(outs).insert(any(InvoiceOutDO.class));
        else verify(ins).insert(any(InvoiceInDO.class));
        assertOriginalBlueAndCapacity(outgoing);
    }
    void assertOriginalBlueAndCapacity(boolean outgoing){
        String side=outgoing?"out":"in";
        assertEquals("CONFIRMED",db.queryForObject("select doc_status from psi_invoice_"+side+" where id=1",String.class));
        assertEquals(0,db.queryForObject("select count(*) from psi_invoice_"+side+" where red_of_id=1",Integer.class));
        assertEquals("INVOICED",db.queryForObject("select invoice_status from psi_stock_"+side+" where id=100",String.class));
        assertEquals(0,new BigDecimal("100").compareTo(linked(outgoing,0)));
    }
}
