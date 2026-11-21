package com.ls.aegis.biz.eoms.h5;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.eoms.mapper.repair.CustomerConfirmationMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CustomerTodoQueryTest {
    JdbcTemplate db;
    CustomerConfirmationMapper mapper;

    @BeforeEach void setup() {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        db = new JdbcTemplate(ds);
        db.execute("create table eoms_repair_apply(id bigint primary key,tenant_id bigint,customer_id bigint,repair_no varchar(64),fault_desc varchar(200),apply_time timestamp,del_flag char(1))");
        db.execute("create table eoms_repair_order(id bigint primary key,apply_id bigint,tenant_id bigint,customer_id bigint,status varchar(32),del_flag char(1))");
        db.execute("create table eoms_finance_event(aggregate_type varchar(16),aggregate_id bigint,action varchar(32))");
        var cfg = new MybatisConfiguration();
        cfg.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
        cfg.addInterceptor(new com.github.pagehelper.PageInterceptor());
        cfg.addMapper(CustomerConfirmationMapper.class);
        mapper = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(cfg)).getMapper(CustomerConfirmationMapper.class);
    }

    @AfterEach void close() { db.execute("SHUTDOWN"); }

    void apply(long id, long tenant, long customer, String deleted) {
        db.update("insert into eoms_repair_apply values(?,?,?,?,'repair',CURRENT_TIMESTAMP,?)", id, tenant, customer, "AP-" + id, deleted);
    }
    void order(long id, long apply, long tenant, long customer, String status, String deleted) {
        db.update("insert into eoms_repair_order values(?,?,?,?,?,?)", id, apply, tenant, customer, status, deleted);
    }

    @Test void onlyUnconfirmedEligibleOrdersBecomeCustomerTodosAndDuplicatesDoNotDuplicateApplications() {
        apply(1,7,10,"0"); apply(2,7,10,"0"); apply(3,7,10,"0");
        order(11,1,7,10,"WAIT_SETTLEMENT","0"); order(12,1,7,10,"FINISHED","0");
        order(21,2,7,10,"PROCESSING","0"); order(31,3,7,10,"FINISHED","0");
        db.update("insert into eoms_finance_event values('ORDER',31,'CUSTOMER_CONFIRM')");
        var rows = mapper.pendingConfirmations(7L,10L);
        assertEquals(1,rows.size()); assertEquals(1L,rows.get(0).getId());
        assertEquals("AP-1",rows.get(0).getRepairNo());
        db.update("insert into eoms_finance_event values('ORDER',11,'CUSTOMER_CONFIRM'),('ORDER',12,'CUSTOMER_CONFIRM')");
        assertTrue(mapper.pendingConfirmations(7L,10L).isEmpty());
    }

    @Test void sessionOwnershipAndSoftDeletionAreAppliedInsideTheQuery() {
        apply(1,7,10,"0"); order(11,1,8,10,"FINISHED","0");
        order(12,1,7,20,"FINISHED","0"); order(13,1,7,10,"FINISHED","1");
        apply(2,8,10,"0"); order(21,2,8,10,"FINISHED","0");
        apply(3,7,20,"0"); order(31,3,7,20,"FINISHED","0");
        apply(4,7,10,"1"); order(41,4,7,10,"FINISHED","0");
        assertTrue(mapper.pendingConfirmations(7L,10L).isEmpty());
        assertEquals(1,mapper.pendingConfirmations(8L,10L).size());
        assertEquals(1,mapper.pendingConfirmations(7L,20L).size());
        assertTrue(mapper.pendingConfirmations(null,10L).isEmpty());
    }

    @Test void pendingConfirmationQuerySupportsDatabasePagingAndTotalCounts() {
        for (long id = 1; id <= 12; id++) {
            apply(id,7,10,"0"); order(100 + id,id,7,10,"FINISHED","0");
        }
        try (var page = com.github.pagehelper.PageHelper.startPage(2,5)) {
            var rows = mapper.pendingConfirmations(7L,10L);
            assertEquals(5,rows.size());
            assertEquals(12,page.getTotal());
            assertEquals(3,page.getPages());
        }
    }
}
