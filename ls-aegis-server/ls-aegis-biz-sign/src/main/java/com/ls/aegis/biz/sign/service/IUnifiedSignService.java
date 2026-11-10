package com.ls.aegis.biz.sign.service;

import com.ls.aegis.biz.sign.domain.vo.UnifiedSignRequest;
import com.ls.aegis.biz.sign.domain.vo.SignResult;

public interface IUnifiedSignService {

    SignResult executeUnifiedSign(UnifiedSignRequest request);
}
