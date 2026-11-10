package com.ls.aegis.biz.eoms.model.query;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

/** Constructed only by the access checker; scope parameters never come from an HTTP request. */
public class EomsFileQuery extends XmlMapperDO {
    private Long tenantId;
    private Long fileId;
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long value) { tenantId = value; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long value) { fileId = value; }
}
