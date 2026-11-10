package com.ls.aegis.biz.psi.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.req.*;
import com.ls.aegis.biz.psi.support.*;
import com.ls.aegis.common.context.*;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
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
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real tenant ledger SQL and transactions; actor-facing cash mappers cannot see another department. */
class RefundTodoLifecycleTest {
    JdbcTemplate db;TransactionTemplate tx;CashCompensationService service;
    final ReceiptMapper receipts=mock(ReceiptMapper.class);final PsiPaymentMapper payments=mock(PsiPaymentMapper.class);
    final StockReturnMapper returns=mock(StockReturnMapper.class);final PsiReceivableMapper ars=mock(PsiReceivableMapper.class);
    final PayableMapper aps=mock(PayableMapper.class);final WriteOffItemMapper allocations=mock(WriteOffItemMapper.class);
    final WriteOffHelper writeOff=mock(WriteOffHelper.class);final IdempotentHelper idem=mock(IdempotentHelper.class);
    final DocNoGenerator numbers=mock(DocNoGenerator.class);final PsiTodoHelper legacyTodos=mock(PsiTodoHelper.class);
    CashLedgerMapper ledger; WriteOffHelper realWriteOff; ReceiptServiceImpl receiptService;
    @BeforeEach @SuppressWarnings("unchecked") void setup()throws Exception{
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        String cash="id bigint primary key,tenant_id bigint,dept_id bigint,unit_id bigint,account_id bigint,doc_no varchar(64),doc_status varchar(20),amount decimal(18,2),unallocated_amount decimal(18,2),refunded_amount decimal(18,2) default 0,refund_source_id bigint,refund_return_id bigint,create_user bigint default 99,operation_sequence bigint default 0,source_sequence bigint,void_sequence bigint,void_user bigint,void_time timestamp,void_reason varchar(500),deleted bigint default 0";
        db.execute("create table psi_receipt("+cash+")");db.execute("create table psi_payment("+cash+")");
        String open="id bigint primary key,tenant_id bigint,unit_id bigint,source_type varchar(32),source_id bigint,tax_inclusive_amount decimal(18,2) default 100,written_off_amount decimal(18,2) default 0,unwritten_amount decimal(18,2) default 100,doc_status varchar(20) default 'OPEN',update_user bigint,update_time timestamp,deleted bigint default 0";
        db.execute("create table psi_receivable("+open+")");db.execute("create table psi_payable("+open+")");
        db.execute("create table psi_write_off_item(id bigint primary key,tenant_id bigint,receipt_id bigint,payment_id bigint,open_type varchar(8),open_id bigint,direction varchar(20),amount decimal(18,2),cash_sequence bigint,write_off_id bigint,reason varchar(500),deleted bigint default 0)");
        db.execute("create table psi_stock_out(id bigint primary key,tenant_id bigint,pay_status varchar(20),update_user bigint,update_time timestamp,deleted bigint default 0)");
        db.execute("create table psi_stock_in(id bigint primary key,tenant_id bigint,pay_status varchar(20),update_user bigint,update_time timestamp,deleted bigint default 0)");
        for(String table:List.of("psi_stock_out","psi_stock_in"))db.update("insert into "+table+"(id,tenant_id) values(700,0),(701,0)");
        db.execute("create table psi_stock_return(id bigint primary key,tenant_id bigint,dept_id bigint,unit_id bigint,doc_no varchar(64),doc_status varchar(20),return_type varchar(32),origin_stock_out_id bigint,origin_stock_in_id bigint,tax_inclusive_amount decimal(18,2),deleted bigint default 0)");
        db.execute("create table psi_bank_statement(tenant_id bigint,match_biz_type varchar(32),match_biz_id bigint,match_status varchar(20),deleted bigint default 0)");
        db.execute("create table psi_todo(id bigint primary key,tenant_id bigint,dept_id bigint,todo_type varchar(32),title varchar(256),biz_type varchar(32),biz_id bigint,biz_no varchar(64),done boolean,create_user bigint,create_time timestamp,update_user bigint,update_time timestamp,deleted bigint default 0)");
        var c=new MybatisConfiguration();c.setMapUnderscoreToCamelCase(true);c.setEnvironment(new Environment("test",new SpringManagedTransactionFactory(),ds));c.addMapper(CashLedgerMapper.class);
        ledger=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(c)).getMapper(CashLedgerMapper.class);
        var actor=new UserContext();actor.setId(99L);actor.setTenantId(0L);actor.setDeptId(12L);UserContextHolder.setContext(actor,false);
        when(idem.execute(anyLong(),anyString(),anyString(),any())).thenAnswer(inv->((Supplier<String>)inv.getArgument(3)).get());
        when(writeOff.listSummaries(any(),any())).thenReturn(List.of());when(numbers.next(anyLong(),anyString())).thenReturn("TEST-REFUND");
        when(receipts.selectByIdForUpdate(anyLong())).thenAnswer(inv->visibleCash("psi_receipt",inv.getArgument(0),ReceiptDO.class,true));
        when(receipts.selectById(anyLong())).thenAnswer(inv->visibleCash("psi_receipt",inv.getArgument(0),ReceiptDO.class,false));
        when(payments.selectByIdForUpdate(anyLong())).thenAnswer(inv->visibleCash("psi_payment",inv.getArgument(0),PaymentDO.class,true));
        when(payments.selectById(anyLong())).thenAnswer(inv->visibleCash("psi_payment",inv.getArgument(0),PaymentDO.class,false));
        when(receipts.updateById(any(ReceiptDO.class))).thenAnswer(inv->save("psi_receipt",inv.getArgument(0)));
        when(payments.updateById(any(PaymentDO.class))).thenAnswer(inv->save("psi_payment",inv.getArgument(0)));
        var ids=new AtomicLong(1000);
        when(receipts.insert(any(ReceiptDO.class))).thenAnswer(inv->{ReceiptDO row=inv.getArgument(0);row.setId(ids.incrementAndGet());insertRefund("psi_receipt",row);return 1;});
        when(payments.insert(any(PaymentDO.class))).thenAnswer(inv->{PaymentDO row=inv.getArgument(0);row.setId(ids.incrementAndGet());insertRefund("psi_payment",row);return 1;});
        when(returns.selectByIdForUpdate(anyLong())).thenAnswer(inv->{var rows=db.query("select * from psi_stock_return where tenant_id=0 and id=? for update",new BeanPropertyRowMapper<>(StockReturnDO.class),inv.<Long>getArgument(0));return rows.isEmpty()?null:rows.get(0);});
        var masters=mock(WriteOffMapper.class);var sequence=new AtomicLong(20000);
        when(masters.insert(any(WriteOffDO.class))).thenAnswer(inv->{WriteOffDO row=inv.getArgument(0);row.setId(sequence.incrementAndGet());return 1;});
        when(allocations.insert(any(WriteOffItemDO.class))).thenAnswer(inv->{WriteOffItemDO row=inv.getArgument(0);row.setId(sequence.incrementAndGet());return db.update("insert into psi_write_off_item(id,tenant_id,receipt_id,payment_id,open_type,open_id,direction,amount,cash_sequence,write_off_id,reason) values(?,?,?,?,?,?,?,?,?,?,?)",row.getId(),row.getTenantId(),row.getReceiptId(),row.getPaymentId(),row.getOpenType().name(),row.getOpenId(),row.getDirection().name(),row.getAmount(),row.getCashSequence(),row.getWriteOffId(),row.getReason());});
        when(allocations.selectById(anyLong())).thenAnswer(inv->{var rows=db.query("select * from psi_write_off_item where id=?",new BeanPropertyRowMapper<>(WriteOffItemDO.class),inv.<Long>getArgument(0));return rows.isEmpty()?null:rows.get(0);});
        when(ars.selectById(anyLong())).thenAnswer(inv->ledger.receivableForCash(0L,inv.getArgument(0)));
        when(aps.selectById(anyLong())).thenAnswer(inv->ledger.payableForCash(0L,inv.getArgument(0)));
        realWriteOff=new WriteOffHelper(masters,allocations,ars,aps,mock(StockOutMapper.class),mock(StockInMapper.class),numbers,mock(PsiTodoCloseSupport.class),ledger);
        doAnswer(inv->{realWriteOff.lockCashFootprint(inv.getArgument(0),inv.getArgument(1),inv.getArgument(2),inv.getArgument(3),inv.getArgument(4));return null;}).when(writeOff).lockCashFootprint(anyBoolean(),anyLong(),anyLong(),anyList(),anyBoolean());
        doAnswer(inv->{realWriteOff.requireCashFootprint(inv.getArgument(0),inv.getArgument(1),inv.getArgument(2),inv.getArgument(3));return null;}).when(writeOff).requireCashFootprint(anyBoolean(),anyLong(),anyLong(),anyCollection());
        when(returns.selectById(anyLong())).thenAnswer(inv->{var rows=db.query("select * from psi_stock_return where tenant_id=0 and id=?",new BeanPropertyRowMapper<>(StockReturnDO.class),inv.<Long>getArgument(0));return rows.isEmpty()?null:rows.get(0);});
        service=new CashCompensationService(receipts,payments,returns,ars,aps,allocations,ledger,writeOff,numbers,idem,legacyTodos);
        receiptService=new ReceiptServiceImpl(receipts,mock(UnitMapper.class),mock(CompanyAccountMapper.class),numbers,idem,realWriteOff,service);
    }
    @AfterEach void clear(){UserContextHolder.clearContext();if(db!=null)db.execute("SHUTDOWN");}
    <T>T visibleCash(String table,Long id,Class<T> type,boolean lock){var rows=db.query("select * from "+table+" where tenant_id=0 and dept_id=12 and id=?"+(lock?" for update":""),new BeanPropertyRowMapper<>(type),id);return rows.isEmpty()?null:rows.get(0);}
    int save(String table,Object row){var cash=cn.hutool.core.bean.BeanUtil.copyProperties(row,CashCompensationService.Cash.class);return db.update("update "+table+" set doc_status=?,unallocated_amount=?,refunded_amount=?,operation_sequence=?,void_sequence=?,void_reason=?,void_time=?,void_user=? where tenant_id=? and id=?",cash.getDocStatus().name(),cash.getUnallocatedAmount(),cash.getRefundedAmount(),cash.getOperationSequence(),cash.getVoidSequence(),cash.getVoidReason(),cash.getVoidTime(),cash.getVoidUser(),cash.getTenantId(),cash.getId());}
    void insertRefund(String table,Object row){var c=cn.hutool.core.bean.BeanUtil.copyProperties(row,CashCompensationService.Cash.class);db.update("insert into "+table+"(id,tenant_id,dept_id,unit_id,doc_no,doc_status,amount,unallocated_amount,refunded_amount,refund_source_id,refund_return_id,source_sequence) values(?,?,?,?,?,?,?,?,?,?,?,?)",c.getId(),c.getTenantId(),c.getDeptId(),c.getUnitId(),c.getDocNo(),c.getDocStatus().name(),c.getAmount(),c.getUnallocatedAmount(),c.getRefundedAmount(),c.getRefundSourceId(),c.getRefundReturnId(),c.getSourceSequence());}
    void seed(boolean receipt){
        db.update("insert into "+(receipt?"psi_receivable":"psi_payable")+"(id,tenant_id,unit_id,source_type,source_id) values(600,0,10,?,700)",receipt?"STOCK_OUT":"STOCK_IN");
        db.update("insert into psi_stock_return(id,tenant_id,dept_id,unit_id,doc_no,doc_status,return_type,origin_stock_out_id,origin_stock_in_id,tax_inclusive_amount) values(500,0,77,10,'TEST-RETURN','CONFIRMED',?,700,700,100)",receipt?"SALE_RETURN":"PURCHASE_RETURN");
        cash(receipt,1,0,12,10,100,"CONFIRMED");events(receipt,1,0,600,100,1);
        db.update("insert into psi_todo(id,tenant_id,dept_id,todo_type,biz_id,biz_no,done) values(1,0,77,?,500,'TEST-RETURN',false)",receipt?"REFUND":"RECEIVE_REFUND");
    }
    void cash(boolean receipt,long id,long tenant,long dept,long unit,int amount,String status){db.update("insert into "+(receipt?"psi_receipt":"psi_payment")+"(id,tenant_id,dept_id,unit_id,doc_no,doc_status,amount,unallocated_amount,refunded_amount,operation_sequence) values(?,?,?,?,?,?,?, ?,0,2)",id,tenant,dept,unit,"TEST-CASH-"+id,status,amount,amount);}
    void events(boolean receipt,long cash,long tenant,long open,int amount,long firstSequence){String column=receipt?"receipt_id":"payment_id";for(int i=0;i<2;i++)db.update("insert into psi_write_off_item(id,tenant_id,"+column+",open_type,open_id,direction,amount,cash_sequence) values(?,?,?,?,?,?,?,?)",cash*100+open*10+i,tenant,cash,receipt?"AR":"AP",open,i==0?"WRITE_OFF":"REVERSE",amount,firstSequence+i);}
    CashActionReq action(){var r=new CashActionReq();r.setReason("TEST correction");r.setIdempotentKey(UUID.randomUUID().toString());return r;}
    boolean done(){return db.queryForObject("select done from psi_todo where id=1",Boolean.class);}
    void voidSource(boolean receipt){tx.executeWithoutResult(s->service.voidCash(receipt,1L,action()));}
    @ParameterizedTest @ValueSource(booleans={true,false}) void reverseRefundThenVoidClosesInapplicableTodoAndRetainsBothCashDocuments(boolean receipt){
        seed(receipt);String source=receipt?"psi_receipt":"psi_payment",child=receipt?"psi_payment":"psi_receipt";
        db.update("update "+source+" set unallocated_amount=0,refunded_amount=100,operation_sequence=3 where id=1");cash(!receipt,10,0,12,10,100,"CONFIRMED");
        db.update("update "+child+" set unallocated_amount=0,refund_source_id=1,refund_return_id=500,source_sequence=3 where id=10");db.update("update psi_todo set done=true where id=1");
        tx.executeWithoutResult(s->service.reverseRefund(!receipt,10L,action()));assertFalse(done());voidSource(receipt);assertTrue(done());
        assertEquals("VOID",db.queryForObject("select doc_status from "+source+" where id=1",String.class));assertEquals("VOID",db.queryForObject("select doc_status from "+child+" where id=10",String.class));
    }
    @Test void anotherDepartmentsLegallyAttributedSourceKeepsTodoOpen(){seed(true);cash(true,2,0,999,10,50,"CONFIRMED");events(true,2,0,600,50,1);voidSource(true);assertFalse(done());verify(ars,never()).selectById(anyLong());verify(aps,never()).selectById(anyLong());verify(allocations,never()).selectList(any());verify(returns,never()).selectList(any());verify(receipts,never()).selectList(any());}
    @Test void anotherTenantCannotKeepThisTenantsTodoOpen(){seed(true);cash(true,2,7,999,10,50,"CONFIRMED");events(true,2,7,600,50,1);voidSource(true);assertTrue(done());}
    @Test void wrongUnitVoidAndLegacyAmbiguousSourcesAreNotLegalRefundCandidates(){seed(true);cash(true,2,0,999,20,50,"CONFIRMED");events(true,2,0,600,50,1);cash(true,3,0,999,10,50,"VOID");events(true,3,0,600,50,1);cash(true,4,0,999,10,50,"CONFIRMED");events(true,4,0,600,50,1);db.update("update psi_write_off_item set cash_sequence=null where receipt_id=4");voidSource(true);assertTrue(done());}
    @Test void oneAmbiguousSourceCannotHideAnotherDepartmentsLegalSource(){seed(true);cash(true,2,0,999,10,50,"CONFIRMED");events(true,2,0,600,50,1);db.update("update psi_write_off_item set cash_sequence=null where receipt_id=2");cash(true,3,0,888,10,50,"CONFIRMED");events(true,3,0,600,50,1);voidSource(true);assertFalse(done());}
    @Test void missingTodoIsCreatedInReturnDepartmentAndRefreshDoesNotDuplicate(){seed(true);db.update("delete from psi_todo");tx.executeWithoutResult(s->{realWriteOff.lockCashFootprint(true,0L,1L,List.of(),false);service.refreshRefundTodos(true,1L);});tx.executeWithoutResult(s->{realWriteOff.lockCashFootprint(true,0L,1L,List.of(),false);service.refreshRefundTodos(true,1L);});assertEquals(1,db.queryForObject("select count(*) from psi_todo",Integer.class));assertEquals(77L,db.queryForObject("select dept_id from psi_todo",Long.class));assertEquals(99L,db.queryForObject("select create_user from psi_todo",Long.class));}
    @Test void voidDoesNotCreateAnInapplicableTodoOrTouchDeletedTodoHistory(){seed(true);db.update("update psi_todo set deleted=id");voidSource(true);assertEquals(1,db.queryForObject("select count(*) from psi_todo",Integer.class));assertFalse(done());}
    @Test void partialRefundExhaustingOnlySourceClosesTodoEvenWithReturnBalanceOutstanding(){seed(true);db.update("update psi_receipt set amount=40,unallocated_amount=40 where id=1");db.update("update psi_write_off_item set amount=40 where receipt_id=1");var r=new CashRefundReq();r.setStockReturnId(500L);r.setAmount(new BigDecimal("40"));r.setReason("TEST partial refund");r.setIdempotentKey(UUID.randomUUID().toString());tx.executeWithoutResult(s->service.refund(true,1L,r));assertTrue(done());assertEquals(0,new BigDecimal("40").compareTo(ledger.sumRefundPayments(0L,500L)));}
    @Test void fullyRefundedReturnClosesEvenWhenAnotherDepartmentStillHasCreditAndPriorSumWasCached(){seed(true);cash(true,2,0,999,10,100,"CONFIRMED");events(true,2,0,600,100,1);var r=new CashRefundReq();r.setStockReturnId(500L);r.setAmount(new BigDecimal("100"));r.setReason("TEST complete return");r.setIdempotentKey(UUID.randomUUID().toString());tx.executeWithoutResult(s->service.refund(true,1L,r));assertTrue(done());}
    @Test void simultaneousVoidOfTwoSourcesSerializesReconciliationWithoutLockingTheOtherCash()throws Exception{
        seed(true);cash(true,2,0,12,10,50,"CONFIRMED");events(true,2,0,600,50,1);

        var pool=Executors.newFixedThreadPool(2);
        try{
            List<Future<?>> tasks=new ArrayList<>();
            for(long id:List.of(1L,2L))tasks.add(pool.submit(()->{var actor=new UserContext();actor.setId(99L);actor.setTenantId(0L);actor.setDeptId(12L);UserContextHolder.setContext(actor,false);try{tx.executeWithoutResult(s->service.voidCash(true,id,action()));}finally{UserContextHolder.clearContext();}}));
            for(var task:tasks)task.get(15,TimeUnit.SECONDS);
            assertTrue(done());assertEquals(2,db.queryForObject("select count(*) from psi_receipt where doc_status='VOID'",Integer.class));
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void sharedCashSpentOnOneOriginClosesOtherOriginsNowInapplicableTodo(){seed(true);db.update("insert into psi_receivable(id,tenant_id,unit_id,source_type,source_id) values(601,0,10,'STOCK_OUT',701)");db.update("insert into psi_stock_return(id,tenant_id,dept_id,unit_id,doc_no,doc_status,return_type,origin_stock_out_id,tax_inclusive_amount) values(501,0,88,10,'TEST-OTHER-RETURN','CONFIRMED','SALE_RETURN',701,100)");db.update("insert into psi_todo(id,tenant_id,dept_id,todo_type,biz_id,biz_no,done) values(2,0,88,'REFUND',501,'TEST-OTHER-RETURN',false)");events(true,1,0,601,100,3);db.update("update psi_receipt set operation_sequence=4 where id=1");var r=new CashRefundReq();r.setStockReturnId(500L);r.setAmount(new BigDecimal("100"));r.setReason("TEST full cash use");r.setIdempotentKey(UUID.randomUUID().toString());tx.executeWithoutResult(s->service.refund(true,1L,r));assertEquals(2,db.queryForObject("select count(*) from psi_todo where done=true",Integer.class));}
    @Test void hiddenOpenAndCashAreStillEligibleForAuthorizedReturnAdvisory(){seed(true);db.update("update psi_receipt set dept_id=999 where id=1");assertTrue(service.hasRefundableCredit(true,600L));verify(ars,never()).selectById(anyLong());verify(aps,never()).selectById(anyLong());verify(allocations,never()).selectList(any());}
    @Test void unauthorizedSourceCannotTriggerGlobalTodoReconciliation(){seed(true);db.update("update psi_receipt set dept_id=999 where id=1");assertThrows(RuntimeException.class,()->voidSource(true));assertFalse(done());}
    void crossedHistory(){
        seed(true);db.update("update psi_write_off_item set amount=50 where receipt_id=1");
        db.update("update psi_receipt set unallocated_amount=50,operation_sequence=3 where id=1");
        db.update("update psi_receivable set written_off_amount=50,unwritten_amount=50 where id=600");
        db.update("insert into psi_receivable(id,tenant_id,unit_id,source_type,source_id,written_off_amount,unwritten_amount) values(601,0,10,'STOCK_OUT',701,50,50)");
        cash(true,2,0,12,10,100,"CONFIRMED");db.update("update psi_receipt set unallocated_amount=50,operation_sequence=3 where id=2");events(true,2,0,601,50,1);
        db.update("insert into psi_write_off_item(id,tenant_id,receipt_id,open_type,open_id,direction,amount,cash_sequence) values(9001,0,1,'AR',601,'WRITE_OFF',50,3),(9002,0,2,'AR',600,'WRITE_OFF',50,3)");
        db.update("insert into psi_stock_return(id,tenant_id,dept_id,unit_id,doc_no,doc_status,return_type,origin_stock_out_id,tax_inclusive_amount) values(501,0,88,10,'TEST-B','CONFIRMED','SALE_RETURN',701,100)");
        db.update("insert into psi_todo(id,tenant_id,dept_id,todo_type,biz_id,biz_no,done) values(2,0,88,'REFUND',501,'TEST-B',false)");
    }
    void worker(Runnable body){var actor=new UserContext();actor.setId(99L);actor.setTenantId(0L);actor.setDeptId(12L);UserContextHolder.setContext(actor,false);try{tx.executeWithoutResult(status->body.run());}finally{UserContextHolder.clearContext();}}
    @Test void legacyCurrentArThenSortedHistoricalArOrderReallyDeadlocksInTwoTransactions()throws Exception{
        crossedHistory();var ready=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);int deadlocks=0,completed=0;
        try{
            List<Future<?>> tasks=new ArrayList<>();
            for(long cashId:List.of(1L,2L))tasks.add(pool.submit(()->worker(()->{
                visibleCash("psi_receipt",cashId,ReceiptDO.class,true);
                long current=cashId==1?601:600;ledger.lockCashStockOut(0L,current==601?701L:700L);ledger.lockReceivableForCash(0L,current);
                try{ready.await(5,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException(e);}
                for(long historical:List.of(600L,601L))ledger.lockReceivableForCash(0L,historical);
            })));
            for(var task:tasks)try{task.get(15,TimeUnit.SECONDS);completed++;}catch(ExecutionException failure){boolean found=false;for(Throwable cause=failure;cause!=null;cause=cause.getCause())if(cause instanceof java.sql.SQLException sql && "40001".equals(sql.getSQLState()))found=true;assertTrue(found,failure.toString());deadlocks++;}
            assertEquals(1,deadlocks);assertEquals(1,completed);
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void fullHistoricalFootprintBeforeCurrentReverseLetsBothCrossedTransactionsCommit()throws Exception{
        crossedHistory();var ready=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);
        when(receipts.selectByIdForUpdate(anyLong())).thenAnswer(inv->{ReceiptDO row=visibleCash("psi_receipt",inv.getArgument(0),ReceiptDO.class,true);ready.await(5,TimeUnit.SECONDS);return row;});
        try{
            List<Future<?>> tasks=new ArrayList<>();for(long cashId:List.of(1L,2L))tasks.add(pool.submit(()->worker(()->{var req=new WriteOffReverseReq();req.setWriteOffItemIds(List.of(cashId==1?9001L:9002L));receiptService.reverse(cashId,req);})));
            for(var task:tasks)task.get(15,TimeUnit.SECONDS);
            assertEquals(2,db.queryForObject("select count(*) from psi_receipt where unallocated_amount=100",Integer.class));
            assertEquals(2,db.queryForObject("select count(*) from psi_receivable where written_off_amount=0 and unwritten_amount=100",Integer.class));
            assertEquals(8,db.queryForObject("select count(*) from psi_write_off_item",Integer.class));
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void positiveWriteoffConsumptionAlsoClosesHistoricalRefundTodo(){
        seed(true);db.update("insert into psi_receivable(id,tenant_id,unit_id,source_type,source_id) values(601,0,10,'STOCK_OUT',701)");
        var item=new WriteOffReq.Item();item.setOpenId(601L);item.setAmount(new BigDecimal("100"));var req=new WriteOffReq();req.setItems(List.of(item));
        tx.executeWithoutResult(status->receiptService.writeOff(1L,req));assertTrue(done());assertEquals(0,db.queryForObject("select unallocated_amount from psi_receipt where id=1",BigDecimal.class).signum());
    }
    @Test void initArNeedsNoStockHeadAndNewTargetStillRequiresCurrentPermission(){
        seed(true);db.update("insert into psi_receivable(id,tenant_id,unit_id,source_type,source_id) values(800,0,10,'INIT_AR',999)");
        var item=new WriteOffReq.Item();item.setOpenId(800L);item.setAmount(BigDecimal.TEN);var req=new WriteOffReq();req.setItems(List.of(item));
        when(ars.selectById(800L)).thenReturn(null);assertThrows(RuntimeException.class,()->tx.executeWithoutResult(status->receiptService.writeOff(1L,req)));
        when(ars.selectById(800L)).thenAnswer(inv->ledger.receivableForCash(0L,800L));tx.executeWithoutResult(status->receiptService.writeOff(1L,req));
        assertEquals(0,new BigDecimal("90").compareTo(db.queryForObject("select unallocated_amount from psi_receipt where id=1",BigDecimal.class)));assertFalse(done());
    }
    @Test void publicRefreshCannotOperateWithoutAuthorizedCashAndPreparedTransactionFootprint(){seed(true);assertThrows(RuntimeException.class,()->tx.executeWithoutResult(status->service.refreshRefundTodos(true,1L)));assertFalse(done());}
    @Test void moneyEntryTransactionsDoNotReuseAnOldRepeatableReadSnapshotForGlobalTodoEligibility()throws Exception{
        for(Class<?> type:List.of(ReceiptServiceImpl.class,PaymentServiceImpl.class))for(var pair:List.of(
            new Object[]{"confirm",DocConfirmReq.class},new Object[]{"writeOff",WriteOffReq.class},new Object[]{"reverse",WriteOffReverseReq.class}))
            assertEquals(org.springframework.transaction.annotation.Isolation.READ_COMMITTED,type.getMethod((String)pair[0],Long.class,(Class<?>)pair[1]).getAnnotation(org.springframework.transaction.annotation.Transactional.class).isolation());
    }
}
