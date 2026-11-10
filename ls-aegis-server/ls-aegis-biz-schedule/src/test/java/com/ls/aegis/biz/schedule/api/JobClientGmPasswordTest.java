package com.ls.aegis.biz.schedule.api;

import cn.hutool.crypto.SecureUtil;
import com.ls.aegis.crypto.config.GmCryptoProperties;
import com.ls.aegis.crypto.impl.SoftwareGmCrypto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * SnailJob 国密口令：线上传 SM3，库存 sha256(SM3)。
 */
class JobClientGmPasswordTest {

    @Test
    void sm3WireThenSha256Stored() {
        GmCryptoProperties properties = new GmCryptoProperties();
        properties.setEnable(true);
        SoftwareGmCrypto crypto = new SoftwareGmCrypto(properties);
        String plain = "admin";
        String wireSm3 = crypto.sm3Hex(plain);
        String stored = SecureUtil.sha256(wireSm3);
        // 与官方 MD5 路径区分，避免误用旧种子
        assertNotEquals(SecureUtil.md5(plain), wireSm3);
        assertEquals(64, wireSm3.length());
        assertEquals(64, stored.length());
        // 运维对照：admin 国密登录摘要与库存
        assertEquals("dc1fd00e3eeeb940ff46f457bf97d66ba7fcc36e0b20802383de142860e76ae6", wireSm3);
        assertEquals("318c70107cb60b3641fc7f720dc0b5ee3a29e06642f7c2799a287e8c42ce84b1", stored);
    }
}
