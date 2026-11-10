package com.ls.aegis.biz.psi.support;

import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.crypto.config.GmCryptoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Explicit, reviewed plaintext-only maintenance. Normal application startup never scans or rewrites account data. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@ConditionalOnProperty(prefix="itls.migration",name="plaintext-account-no-enabled",havingValue="true",matchIfMissing=false)
@RequiredArgsConstructor
public class AccountNoSm4Migrator implements ApplicationRunner {
    private static final Set<String> TABLES=Set.of("psi_company_account","psi_unit_pay","psi_unit_invoice");
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<IGmCrypto> gmCryptoProvider;
    private final GmCryptoProperties gmCryptoProperties;
    @Value("${itls.migration.plaintext-account-no-allowlist:}")
    private String plaintextAllowlist;

    @Override
    @Transactional(rollbackFor=Exception.class)
    public void run(ApplicationArguments args) {
        if(!gmCryptoProperties.isEnable())throw new BusinessException("受控账号迁移要求国密已启用");
        IGmCrypto gm=gmCryptoProvider.getIfAvailable();
        if(gm==null || !gm.isEnabled())throw new BusinessException("受控账号迁移缺少可用国密实现");
        Map<String,Set<Long>> allowlist=parseAllowlist(plaintextAllowlist);
        List<Rewrite> rewrites=new ArrayList<>();
        // Inspect the entire approved batch before writing anything. Never assume failed SM4 decryption means plaintext.
        for(var entry:allowlist.entrySet()) {
            String placeholders=String.join(",",Collections.nCopies(entry.getValue().size(),"?"));
            List<Map<String,Object>> rows=jdbcTemplate.queryForList("SELECT id,account_no AS accountNo FROM `"+entry.getKey()+"` WHERE deleted=0 AND id IN ("+placeholders+") FOR UPDATE",entry.getValue().toArray());
            if(rows.size()!=entry.getValue().size())throw new BusinessException("受控账号迁移白名单包含缺失或已删行，请核对");
            for(var row:rows) {
                String value=(String)row.get("accountNo");
                if(needsEncrypt(value,gm))rewrites.add(new Rewrite(entry.getKey(),((Number)row.get("id")).longValue(),value,gm.sm4EncryptToBase64(value.getBytes(StandardCharsets.UTF_8))));
            }
        }
        for(Rewrite rewrite:rewrites) {
            int changed=jdbcTemplate.update("UPDATE `"+rewrite.table()+"` SET account_no=? WHERE id=? AND deleted=0 AND account_no=?",rewrite.cipher(),rewrite.id(),rewrite.original());
            if(changed!=1)throw new BusinessException("受控账号迁移期间记录已变化，已中止");
        }
        log.info("受控明文账号迁移完成，处理 {} 行；未输出账号或密钥",rewrites.size());
    }

    /** Current SM4 stays unchanged. Only explicitly approved ASCII numeric plaintext (6-24 digits) is supported. */
    public static boolean needsEncrypt(String value,IGmCrypto gm) {
        if(value==null || value.isBlank())return false;
        try {
            byte[] plain=gm.sm4DecryptFromBase64(value);
            if(value.equals(gm.sm4EncryptToBase64(plain)))return false;
        }catch(Exception notCurrentSm4){/* Classification continues; failure is not evidence of plaintext. */}
        boolean cipherLike=false;
        try{byte[] decoded=Base64.getDecoder().decode(value);cipherLike=decoded.length>=16 && decoded.length%16==0;}catch(IllegalArgumentException ignored){ }
        if(cipherLike || !value.matches("[0-9]{6,24}"))throw new BusinessException("账号迁移遇到未知或疑似历史密文，禁止自动包装；请提供审计后的明文映射");
        return true;
    }
    private static Map<String,Set<Long>> parseAllowlist(String text) {
        if(text==null || text.isBlank())throw new BusinessException("受控账号迁移必须提供明确表及ID白名单");
        Map<String,Set<Long>> result=new TreeMap<>();
        try {
            for(String group:text.split(";")) {
                String[] pair=group.split(":",-1);if(pair.length!=2 || !TABLES.contains(pair[0]))throw new IllegalArgumentException();
                Set<Long> ids=result.computeIfAbsent(pair[0],k->new TreeSet<>());
                for(String id:pair[1].split(",",-1)){long value=Long.parseLong(id);if(value<=0)throw new IllegalArgumentException();ids.add(value);}
            }
            if(result.values().stream().mapToInt(Set::size).sum()>1000)throw new IllegalArgumentException();
        }catch(RuntimeException invalid){throw new BusinessException("账号迁移白名单格式无效或超过1000行");}
        return result;
    }
    private record Rewrite(String table,Long id,String original,String cipher) {
        @Override public String toString(){return "Rewrite[redacted]";}
    }
}
