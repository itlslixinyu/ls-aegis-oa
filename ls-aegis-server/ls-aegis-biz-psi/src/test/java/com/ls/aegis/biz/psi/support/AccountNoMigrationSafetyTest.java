package com.ls.aegis.biz.psi.support;

import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.crypto.config.GmCryptoProperties;
import com.ls.aegis.crypto.impl.SoftwareGmCrypto;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import top.continew.starter.core.exception.BusinessException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountNoMigrationSafetyTest {
    private JdbcTemplate jdbc;
    private IGmCrypto gm;
    private AccountNoSm4Migrator runner;
    @BeforeEach @SuppressWarnings("unchecked") void setup(){
        GmCryptoProperties props=new GmCryptoProperties();byte[] key=new byte[16],iv=new byte[16];new SecureRandom().nextBytes(key);new SecureRandom().nextBytes(iv);
        props.getSm4().setKey(Base64.getEncoder().encodeToString(key));props.getSm4().setIv(Base64.getEncoder().encodeToString(iv));gm=new SoftwareGmCrypto(props);
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");ds.setDriverClassName("org.h2.Driver");jdbc=new JdbcTemplate(ds);
        jdbc.execute("create table psi_company_account(id bigint primary key,account_no varchar(512),deleted bigint default 0)");
        ObjectProvider<IGmCrypto> provider=mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(gm);runner=new AccountNoSm4Migrator(jdbc,provider,props);
    }
    @AfterEach void cleanup(){jdbc.execute("SHUTDOWN");}
    @Test void ordinaryStartupDoesNotRegisterMigrationRunner(){
        new ApplicationContextRunner().withUserConfiguration(AccountNoSm4Migrator.class).run(ctx->assertFalse(ctx.containsBean("accountNoSm4Migrator")));
        new ApplicationContextRunner().withPropertyValues("itls.migration.plaintext-account-no-enabled=false").withUserConfiguration(AccountNoSm4Migrator.class).run(ctx->assertFalse(ctx.containsBean("accountNoSm4Migrator")));
    }
    @Test void approvedPlaintextOnlyChangesExplicitIdsAndCurrentSm4RemainsStable(){
        jdbc.update("insert into psi_company_account values(1,'6222021234563456',0),(2,'6222021234563457',0)");
        allow("psi_company_account:1");runner.run(null);String cipher=value(1);
        assertNotEquals("6222021234563456",cipher);assertEquals("6222021234563456",new String(gm.sm4DecryptFromBase64(cipher),StandardCharsets.UTF_8));
        assertEquals("6222021234563457",value(2));runner.run(null);assertEquals(cipher,value(1));
    }
    @Test void unknownValueAbortsEntirePreflightBeforeAnyPlaintextRewrite(){
        jdbc.update("insert into psi_company_account values(1,'6222021234563456',0),(2,'unknown-cipher-value',0)");allow("psi_company_account:1,2");
        assertThrows(BusinessException.class,()->runner.run(null));assertEquals("6222021234563456",value(1));assertEquals("unknown-cipher-value",value(2));
    }
    @Test void realAesCiphertextIsNeverWrappedWithAnotherLayer() throws Exception {
        byte[] key=new byte[16],iv=new byte[16];new SecureRandom().nextBytes(key);new SecureRandom().nextBytes(iv);
        Cipher aes=Cipher.getInstance("AES/CBC/PKCS5Padding");aes.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new IvParameterSpec(iv));
        String historical=Base64.getEncoder().encodeToString(aes.doFinal("6222021234563456".getBytes(StandardCharsets.UTF_8)));
        jdbc.update("insert into psi_company_account values(1,?,0)",historical);allow("psi_company_account:1");
        try{runner.run(null);}catch(BusinessException expectedUnknownCipher){ }
        assertEquals(historical,value(1),"Unknown/AES input must never be rewritten");
    }
    @Test void missingOrUnsafeAllowlistCannotStartWriting(){
        jdbc.update("insert into psi_company_account values(1,'6222021234563456',0)");
        for(String spec:new String[]{"","unexpected_table:1","psi_company_account:0","psi_company_account:1;drop table:2"}){
            allow(spec);assertThrows(BusinessException.class,()->runner.run(null));assertEquals("6222021234563456",value(1));
        }
    }
    private void allow(String value){ReflectionTestUtils.setField(runner,"plaintextAllowlist",value);}
    private String value(long id){return jdbc.queryForObject("select account_no from psi_company_account where id=?",String.class,id);}
}
