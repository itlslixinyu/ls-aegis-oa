package com.ls.aegis.biz.psi.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisParameterHandler;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.ls.aegis.biz.psi.model.entity.ReceiptDO;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.mybatis.base.mapper.DataPermissionMapper;
import com.ls.aegis.mybatis.base.model.entity.BaseDO;
import com.ls.aegis.mybatis.config.MyBatisPlusMetaObjectHandler;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 使用真正 MapperProxy、MP 生成的逻辑删除 SQL 和参数处理器，不连接数据库。 */
class LogicalDeleteBindingTest {
    private static final Long ROW_ID = 908840135997522261L;
    private static final Long OPERATOR_ID = 777L;

    static Stream<Class<?>> affectedMappers() {
        return List.<Class<?>>of(ReceiptMapper.class, PsiPaymentMapper.class,
            StockInMapper.class, StockOutMapper.class, StockReturnMapper.class,
            StockTransferMapper.class, StockCheckMapper.class, StockAdjustMapper.class,
            InitStockMapper.class, InitReceivableMapper.class, InitPayableMapper.class,
            InitPrepayMapper.class, InvoiceInMapper.class, InvoiceOutMapper.class,
            ContractPurchaseMapper.class, ContractSaleMapper.class).stream();
    }

    @ParameterizedTest(name = "{0}: scalar ID creates an auditable entity")
    @MethodSource("affectedMappers")
    void scalarDeleteBindsIdAndAuditFields(Class<?> mapperType) throws Exception {
        UserContext context = new UserContext();
        context.setId(OPERATOR_ID);
        UserContextHolder.setContext(context, false);
        try {
            AtomicInteger executions = new AtomicInteger();
            Object mapper = mapper(mapperType, true, executions);
            assertEquals(1, mapperType.getMethod("deleteById", Serializable.class).invoke(mapper, ROW_ID));
            assertEquals(1, executions.get());
        } finally {
            UserContextHolder.clearContext();
        }
    }

    @Test
    void directEntityDeleteRetainsAuditBindingAndBothPermissionAnnotations() throws Exception {
        assertNotNull(DataPermissionMapper.class.getMethod("deleteById", Serializable.class).getAnnotation(DataPermission.class));
        assertNotNull(DataPermissionMapper.class.getMethod("deleteById", Object.class).getAnnotation(DataPermission.class));
        UserContext context = new UserContext();
        context.setId(OPERATOR_ID);
        UserContextHolder.setContext(context, false);
        try {
            ReceiptDO entity = new ReceiptDO();
            entity.setId(ROW_ID);
            AtomicInteger executions = new AtomicInteger();
            ReceiptMapper mapper = (ReceiptMapper) mapper(ReceiptMapper.class, true, executions);
            assertEquals(1, mapper.deleteById(entity));
            assertEquals(OPERATOR_ID, entity.getUpdateUser());
            assertNotNull(entity.getUpdateTime());
            assertEquals(1, executions.get());
        } finally {
            UserContextHolder.clearContext();
        }
    }

    @Test
    void oldAbstractScalarOverrideReproducesMissingUpdateUserBinding() {
        UnsafeScalarMapper mapper = (UnsafeScalarMapper) mapper(UnsafeScalarMapper.class, false, new AtomicInteger());
        BindingException failure = assertThrows(BindingException.class, () -> mapper.deleteById(ROW_ID));
        assertTrue(failure.getMessage().contains("updateUser"));
    }

    /** 精确保留原先破坏 MP 默认方法的声明，证明此回归能区分修复前后。 */
    interface UnsafeScalarMapper extends DataPermissionMapper<ReceiptDO> {
        @Override
        int deleteById(@Param("id") Serializable id);
    }

    private Object mapper(Class<?> mapperType, boolean entityExpected, AtomicInteger executions) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        GlobalConfig global = new GlobalConfig();
        global.setDbConfig(new GlobalConfig.DbConfig()
            .setLogicDeleteField("deleted").setLogicNotDeleteValue("0").setLogicDeleteValue("id"));
        global.setMetaObjectHandler(new MyBatisPlusMetaObjectHandler());
        GlobalConfigUtils.setGlobalConfig(configuration, global);
        configuration.addMapper(mapperType);
        SqlSession session = mock(SqlSession.class);
        when(session.getConfiguration()).thenReturn(configuration);
        when(session.update(anyString(), any())).thenAnswer(call -> {
            String statementId = call.getArgument(0);
            Object parameter = call.getArgument(1);
            MappedStatement statement = configuration.getMappedStatement(statementId);
            BoundSql bound = statement.getBoundSql(parameter);
            String sql = bound.getSql().replaceAll("\\s+", " ").toLowerCase();
            assertTrue(sql.startsWith("update "), sql);
            assertTrue(sql.contains("deleted=id"), sql);
            assertTrue(sql.matches(".*where id\\s*=\\s*\\?.*deleted=0.*"), sql);
            MybatisParameterHandler handler = new MybatisParameterHandler(statement, parameter, bound);
            handler.setParameters(mock(PreparedStatement.class));
            if (entityExpected) {
                BaseDO entity = assertInstanceOf(BaseDO.class, parameter);
                assertEquals(ROW_ID, entity.getId());
                assertEquals(OPERATOR_ID, entity.getUpdateUser());
                assertNotNull(entity.getUpdateTime());
                assertTrue(bound.getParameterMappings().stream().anyMatch(p -> "updateUser".equals(p.getProperty())));
                assertTrue(bound.getParameterMappings().stream().anyMatch(p -> "updateTime".equals(p.getProperty())));
                assertTrue(bound.getParameterMappings().stream().anyMatch(p -> "id".equals(p.getProperty())));
            }
            executions.incrementAndGet();
            return 1;
        });
        return configuration.getMapper(mapperType, session);
    }
}
