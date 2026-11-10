package com.ls.aegis.biz.sign.service.provider;

import com.ls.aegis.biz.sign.domain.vo.ManagedSignMaterial;

public interface KeyProvider {

    ManagedSignMaterial loadBySealId(Long sealId) throws Exception;
}
