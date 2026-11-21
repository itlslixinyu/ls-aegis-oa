package com.ls.aegis.starter.support.docno;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import com.ls.aegis.common.api.docno.DocNoPrefixApi;

/**
 * RBAC 未挂载时兜底：直接返回默认前缀
 */
@Service
@ConditionalOnMissingBean(DocNoPrefixApi.class)
public class DefaultDocNoPrefixApi implements DocNoPrefixApi {

    @Override
    public String resolvePrefix(String optionCode, String defaultPrefix) {
        return defaultPrefix;
    }
}
