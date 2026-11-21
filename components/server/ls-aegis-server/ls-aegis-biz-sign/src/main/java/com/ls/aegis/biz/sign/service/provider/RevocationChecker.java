package com.ls.aegis.biz.sign.service.provider;

public interface RevocationChecker {

    boolean isRevoked(Long certId);
}
