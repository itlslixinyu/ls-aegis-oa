package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignCertRequest;

/**
 * 证书申请 Mapper。
 */
public interface SignCertRequestMapper {

    SignCertRequest selectSignCertRequestByRequestId(Long requestId);

    int insertSignCertRequest(SignCertRequest request);
}
