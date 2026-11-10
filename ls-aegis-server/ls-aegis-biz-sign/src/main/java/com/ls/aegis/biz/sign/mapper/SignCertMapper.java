package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignCert;
import org.apache.ibatis.annotations.Param;

/**
 * 托管证书 Mapper。
 */
public interface SignCertMapper {

    SignCert selectSignCertByCertId(Long certId);

    SignCert selectActiveBySealId(@Param("sealId") Long sealId);

    int insertSignCert(SignCert cert);

    int updateSignCertStatus(@Param("certId") Long certId, @Param("status") String status);
}
