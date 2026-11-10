package com.ls.aegis.biz.psi.mapper;

import cn.hutool.extra.spring.SpringUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ls.aegis.biz.file.access.FileAccessContext;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.psi.access.PsiFileAccessChecker;
import com.ls.aegis.biz.psi.mapper.file.PsiFileAccessMapper;
import com.ls.aegis.common.config.TenantExtensionProperties;
import com.ls.aegis.common.context.*;
import com.ls.aegis.common.enums.DataScopeEnum;
import com.ls.aegis.mybatis.config.DefaultDataPermissionUserDataProvider;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import top.continew.starter.extension.datapermission.handler.DefaultDataPermissionHandler;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PsiFileAccessPermissionTest {
    GenericApplicationContext context; PsiFileAccessMapper mapper;
    static final String[] KINDS = {"contractSale", "contractPurchase", "invoiceIn", "invoiceOut", "printTemplate"};
    public static int findInSet(String value, String csv) {
        return csv != null && Arrays.asList(csv.split(",")).contains(value) ? 1 : 0;
    }
    void actor(DataScopeEnum scope) {
        var a=new UserContext();a.setId(100L);a.setDeptId(10L);a.setTenantId(1L);
        a.setRoles(Set.of(new RoleContext(9L,"normal",scope)));UserContextHolder.setContext(a);
    }
    @BeforeEach void setup() throws Exception {
        context=new GenericApplicationContext();context.registerBean(TenantExtensionProperties.class);context.refresh();new SpringUtil().setApplicationContext(context);
        var request=new org.springframework.mock.web.MockHttpServletRequest();var response=new org.springframework.mock.web.MockHttpServletResponse();
        cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff.setModelBox(new cn.dev33.satoken.servlet.model.SaRequestForServlet(request),new cn.dev33.satoken.servlet.model.SaResponseForServlet(response),new cn.dev33.satoken.servlet.model.SaStorageForServlet(request));
        StpUtil.login(100L);
        actor(DataScopeEnum.DEPT);
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        var db=new JdbcTemplate(ds);
        db.execute("CREATE ALIAS FIND_IN_SET FOR 'com.ls.aegis.biz.psi.mapper.PsiFileAccessPermissionTest.findInSet'");
        for(String table:List.of("psi_contract_sale","psi_contract_purchase","psi_invoice_in","psi_invoice_out","psi_print_template")) {
            db.execute("CREATE TABLE "+table+"(id BIGINT,tenant_id BIGINT,dept_id BIGINT,create_user BIGINT,deleted BIGINT,file_id VARCHAR(100))");
            db.execute("INSERT INTO "+table+" VALUES(1,1,10,200,0,'51'),(2,1,20,100,0,'52'),(3,2,10,100,0,'53'),(4,1,10,100,4,'54')");
        }
        var cfg=new MybatisConfiguration();
        var plugin=new MybatisPlusInterceptor();plugin.addInnerInterceptor(new DataPermissionInterceptor(new DefaultDataPermissionHandler(new DefaultDataPermissionUserDataProvider())));
        cfg.addInterceptor(plugin);
        var factory=new MybatisSqlSessionFactoryBean();factory.setConfiguration(cfg);factory.setDataSource(ds);
        factory.setMapperLocations(new ClassPathResource("mapper/psi/PsiFileAccessMapper.xml"));
        mapper=new SqlSessionTemplate(factory.getObject()).getMapper(PsiFileAccessMapper.class);
    }
    @AfterEach void cleanup(){StpUtil.logout();cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff.clearModelBox();UserContextHolder.clearContext();context.close();}
    @Test void departmentCannotReadAnotherDepartmentsReferencedFile(){for(String k:KINDS){assertEquals(1,mapper.countAccessible(51L,1L,k));assertEquals(0,mapper.countAccessible(52L,1L,k));}}
    @Test void selfPermissionUsesReferenceOwner(){actor(DataScopeEnum.SELF);for(String k:KINDS){assertEquals(0,mapper.countAccessible(51L,1L,k));assertEquals(1,mapper.countAccessible(52L,1L,k));}}
    @Test void allScopeStillPreservesExplicitTenantAndDeletedFilters(){actor(DataScopeEnum.ALL);for(String k:KINDS){assertEquals(1,mapper.countAccessible(52L,1L,k));assertEquals(0,mapper.countAccessible(53L,1L,k));assertEquals(0,mapper.countAccessible(54L,1L,k));}}
    @Test void unrecognizedKindCannotFallBackToGlobalReferences(){assertEquals(0,mapper.countAccessible(51L,1L,"unexpected"));}
    @Test void referenceAloneDoesNotGrantLoggedInUserBusinessPermission(){
        var checker=new PsiFileAccessChecker();var mocked=mock(PsiFileAccessMapper.class);ReflectionTestUtils.setField(checker,"psiFileAccessMapper",mocked);
        var file=new FileDO();file.setId(51L);var access=FileAccessContext.forPlatformUser();
        try(var auth=mockStatic(StpUtil.class)){
            auth.when(StpUtil::getLoginIdAsString).thenReturn("100");
            assertFalse(checker.canAccess(file,access));verifyNoInteractions(mocked);
            auth.when(()->StpUtil.hasPermission("psi:invoice-in:list")).thenReturn(true);
            when(mocked.countAccessible(51L,1L,"invoiceIn")).thenReturn(1);
            assertTrue(checker.canAccess(file,access));verify(mocked).countAccessible(51L,1L,"invoiceIn");
        }
    }
    @Test void staleContextCannotUseAnotherRealIdentity(){
        var checker=new PsiFileAccessChecker();var mocked=mock(PsiFileAccessMapper.class);ReflectionTestUtils.setField(checker,"psiFileAccessMapper",mocked);
        var file=new FileDO();file.setId(51L);var access=FileAccessContext.forPlatformUser();
        try(var auth=mockStatic(StpUtil.class)){auth.when(StpUtil::getLoginIdAsString).thenReturn("999");assertFalse(checker.canAccess(file,access));verifyNoInteractions(mocked);}
    }
}
