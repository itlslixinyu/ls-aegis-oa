package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignCertRevocation;

import java.util.List;

/**
 * 证书吊销 Mapper。
 */
public interface SignCertRevocationMapper {

    List<SignCertRevocation> selectByCertId(Long certId);

    int insertSignCertRevocation(SignCertRevocation revocation);
}
