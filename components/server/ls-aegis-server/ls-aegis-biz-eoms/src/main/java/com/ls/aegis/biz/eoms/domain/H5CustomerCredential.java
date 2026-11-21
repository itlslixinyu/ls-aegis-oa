package com.ls.aegis.biz.eoms.domain;

/** Independent, immutable-login member of one PSI customer. Never serialize password material. */
@lombok.Data
public class H5CustomerCredential {
    private Long id;
    private Long tenantId;
    private Long customerId;
    private String loginName;
    private String displayName;
    private String accountRole;
    private Integer status;
    private Long authVersion;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    private String passwordHash;
}
