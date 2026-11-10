package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ls.aegis.biz.psi.enums.PsiImportPolicyEnum;
import com.ls.aegis.biz.psi.mapper.CompanyAccountMapper;
import com.ls.aegis.biz.psi.model.entity.CompanyAccountDO;
import com.ls.aegis.biz.psi.model.req.*;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.support.PsiImportHelper;
import com.ls.aegis.common.context.*;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.crypto.config.GmCryptoProperties;
import com.ls.aegis.crypto.encryptor.Sm4Encryptor;
import com.ls.aegis.crypto.impl.SoftwareGmCrypto;
import com.ls.aegis.mybatis.config.MyBatisPlusMetaObjectHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import top.continew.starter.encrypt.context.CryptoContext;
import top.continew.starter.encrypt.field.autoconfigure.FieldEncryptProperties;
import top.continew.starter.encrypt.field.interceptor.*;
import top.continew.starter.encrypt.field.util.EncryptHelper;
import top.continew.starter.extension.datapermission.enums.DataScope;
import top.continew.starter.extension.datapermission.handler.DefaultDataPermissionHandler;
import top.continew.starter.extension.datapermission.model.*;
import top.continew.starter.extension.datapermission.provider.DataPermissionUserDataProvider;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real SM4, field interceptors, tenant/data-scope plugins, CRUD/import and unique index; cache only is mocked. */
class CompanyAccountImportCryptoTest {
    private static final String ACCOUNT="ITLS-TEST-ACCOUNT-0001";
    private static jakarta.validation.ValidatorFactory validators;
    @BeforeAll static void validatorFactory(){validators=jakarta.validation.Validation.buildDefaultValidatorFactory();}
    @AfterAll static void closeValidatorFactory(){validators.close();}
    private GenericApplicationContext spring;
    private JdbcTemplate jdbc;
    private CompanyAccountMapper mapper;
    private CompanyAccountServiceImpl service;
    private TransactionTemplate tx;

    @BeforeEach void setup() throws Exception {
        GmCryptoProperties props=new GmCryptoProperties(); byte[] key=new byte[16],iv=new byte[16];
        new SecureRandom().nextBytes(key);new SecureRandom().nextBytes(iv);
        props.getSm4().setKey(Base64.getEncoder().encodeToString(key));props.getSm4().setIv(Base64.getEncoder().encodeToString(iv));
        spring=new GenericApplicationContext();spring.registerBean(IGmCrypto.class,()->new SoftwareGmCrypto(props));spring.registerBean(GmCryptoProperties.class,()->props);spring.registerBean(jakarta.validation.Validator.class,validators::getValidator);spring.refresh();
        new SpringUtil().setApplicationContext(spring);EncryptHelper.init(new FieldEncryptProperties());
        UserContext user=new UserContext();user.setId(99L);user.setDeptId(12L);user.setTenantId(0L);UserContextHolder.setContext(user,false);
        DriverManagerDataSource ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");ds.setDriverClassName("org.h2.Driver");
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        MybatisConfiguration config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);
        config.setDefaultEnumTypeHandler(top.continew.starter.data.handler.CompositeBaseEnumTypeHandler.class);
        var global=GlobalConfigUtils.getGlobalConfig(config);global.getDbConfig().setLogicDeleteField("deleted").setLogicNotDeleteValue("0").setLogicDeleteValue("id");global.setMetaObjectHandler(new MyBatisPlusMetaObjectHandler());
        MybatisPlusInterceptor chain=new MybatisPlusInterceptor();
        chain.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){@Override public Expression getTenantId(){return new LongValue(UserContextHolder.getTenantId());}}));
        chain.addInnerInterceptor(new DataPermissionInterceptor(new DefaultDataPermissionHandler(new DataPermissionUserDataProvider(){
            @Override public boolean isFilter(){return true;}
            @Override public UserData getUserData(){return new UserData(99L,UserContextHolder.getContext().getDeptId(),Set.of(new RoleData(1L,DataScope.DEPT)));}
        })));
        chain.addInnerInterceptor(new MyBatisEncryptInterceptor());config.addInterceptor(new MyBatisDecryptInterceptor());config.addInterceptor(chain);config.addMapper(CompanyAccountMapper.class);
        MybatisSqlSessionFactoryBean factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(config);
        mapper=new SqlSessionTemplate(factory.getObject()).getMapper(CompanyAccountMapper.class);
        var table=TableInfoHelper.getTableInfo(CompanyAccountDO.class);StringBuilder ddl=new StringBuilder("create table psi_company_account(id bigint primary key");
        for(var field:table.getFieldList()) {
            String type=field.getPropertyType()==Long.class ? "bigint" : field.getPropertyType()==LocalDateTime.class ? "timestamp" : field.getProperty().equals("status") ? "int" : field.getProperty().equals("accountNo") ? "varchar(128)" : "varchar(512)";
            ddl.append(',').append(field.getColumn()).append(' ').append(type);if(field.getProperty().equals("deleted"))ddl.append(" default 0");
        }
        ddl.append(",unique(tenant_id,account_no,deleted))");jdbc.execute(ddl.toString());
        service=new CompanyAccountServiceImpl();ReflectionTestUtils.setField(service,"baseMapper",mapper);
    }
    @AfterEach void cleanup(){UserContextHolder.clearContext();jdbc.execute("SHUTDOWN");spring.close();new SpringUtil().setApplicationContext(null);}

    @Test void actualFieldEncryptionAndCustomLockDecryptWorkWhilePlainWrapperDoesNotMatch() {
        Long id=tx.execute(s->service.create(request("original")));String stored=cipher(id);
        assertNotEquals(ACCOUNT,stored);assertEquals(encrypt(ACCOUNT),stored);
        assertNull(mapper.selectOne(Wrappers.lambdaQuery(CompanyAccountDO.class).eq(CompanyAccountDO::getAccountNo,ACCOUNT)));
        tx.executeWithoutResult(s->assertEquals(ACCOUNT,mapper.selectByIdForUpdate(id).getAccountNo()));
    }
    @Test void renamedAccountImportSkipAndUpdateFindEncryptedIdentityWithoutDuplicateOrDoubleEncryption() {
        Long id=tx.execute(s->service.create(request("original")));String stored=cipher(id);
        PsiImportResp skipped=importOne("renamed",PsiImportPolicyEnum.SKIP);
        assertEquals(1,skipped.getSkipRows());assertEquals(0,skipped.getInsertRows());
        assertEquals("original",jdbc.queryForObject("select account_name from psi_company_account where id=?",String.class,id));
        PsiImportResp updated=importOne("renamed",PsiImportPolicyEnum.UPDATE);
        assertEquals(1,updated.getUpdateRows());assertEquals(0,updated.getInsertRows());assertEquals(1,count());
        assertEquals("renamed",jdbc.queryForObject("select account_name from psi_company_account where id=?",String.class,id));assertEquals(stored,cipher(id));
        assertEquals(12L,jdbc.queryForObject("select dept_id from psi_company_account where id=?",Long.class,id));
    }
    @Test void uniqueCiphertextAndDepartmentScopePreventImportFromOverwritingInvisibleAccount() {
        seed(100L,0L,20L,"invisible");
        tx.executeWithoutResult(s->assertNull(mapper.selectByIdForUpdate(100L)));
        assertThrows(org.springframework.dao.DuplicateKeyException.class,()->importOne("renamed",PsiImportPolicyEnum.UPDATE));
        assertEquals(1,count());assertEquals("invisible",jdbc.queryForObject("select account_name from psi_company_account where id=100",String.class));
        assertEquals(20L,jdbc.queryForObject("select dept_id from psi_company_account where id=100",Long.class));
    }
    @Test void sameAccountNumberInOtherTenantIsNotMistakenForCurrentTenantDuplicate() {
        seed(100L,2L,12L,"other tenant");
        tx.executeWithoutResult(s->assertNull(mapper.selectByIdForUpdate(100L)));
        PsiImportResp created=importOne("own tenant",PsiImportPolicyEnum.SKIP);assertEquals(1,created.getInsertRows());assertEquals(2,count());
        assertEquals(1,jdbc.queryForObject("select count(*) from psi_company_account where tenant_id=0 and dept_id=12",Integer.class));
        assertEquals("other tenant",jdbc.queryForObject("select account_name from psi_company_account where id=100",String.class));
    }
    @Test void utf8LengthIsCheckedBeforeEncryptionExceedsTheReal128CharacterColumn() {
        CompanyAccountReq boundary=request("boundary");boundary.setAccountNo("aa"+"\u5361".repeat(31));
        Long id=tx.execute(s->service.create(boundary));assertEquals(128,cipher(id).length());
        CompanyAccountReq oversized=request("too long");oversized.setAccountNo("\u5361".repeat(64));
        assertThrows(top.continew.starter.core.exception.BusinessException.class,()->tx.execute(s->service.create(oversized)));
        assertEquals(1,count());
    }
    private PsiImportResp importOne(String name,PsiImportPolicyEnum policy){
        AccountImportRowReq row=new AccountImportRowReq();row.setAccountName(name);row.setBankName("synthetic bank");row.setAccountNo(ACCOUNT);
        PsiImportReq req=new PsiImportReq();req.setImportKey("synthetic-cache-key");req.setDuplicatePolicy(policy);req.setDefaultStatus(1);
        try(MockedStatic<PsiImportHelper> cache=mockStatic(PsiImportHelper.class)){
            cache.when(()->PsiImportHelper.loadRows(req.getImportKey(),AccountImportRowReq.class)).thenReturn(List.of(row));
            cache.when(()->PsiImportHelper.parseStatus(null,1)).thenReturn(DisEnableStatusEnum.ENABLE);
            return tx.execute(s->service.importData(req));
        }
    }
    private CompanyAccountReq request(String name){CompanyAccountReq req=new CompanyAccountReq();req.setAccountName(name);req.setBankName("synthetic bank");req.setAccountNo(ACCOUNT);req.setStatus(DisEnableStatusEnum.ENABLE);return req;}
    private void seed(Long id,Long tenant,Long dept,String name){jdbc.update("insert into psi_company_account(id,tenant_id,dept_id,account_name,bank_name,account_no,status,deleted) values(?,?,?,?,?,?,1,0)",id,tenant,dept,name,"synthetic bank",encrypt(ACCOUNT));}
    private String cipher(Long id){return jdbc.queryForObject("select account_no from psi_company_account where id=?",String.class,id);}
    private static String encrypt(String value){return new Sm4Encryptor(new CryptoContext()).encrypt(value);}
    private int count(){return jdbc.queryForObject("select count(*) from psi_company_account",Integer.class);}
}
