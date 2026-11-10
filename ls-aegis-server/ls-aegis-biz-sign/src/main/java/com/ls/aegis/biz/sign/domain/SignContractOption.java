package com.ls.aegis.biz.sign.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 项目可选合同（销售 / 采购 / 维修）
 */
public class SignContractOption {

    /** SALE / PURCHASE / REPAIR */
    private String contractType;
    /** 雪花 ID 下发字符串，避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String contractNo;
    private String contractName;
    private String partnerName;
    /** 合同附件 sys_file.id，逗号分隔 */
    private String fileId;

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
