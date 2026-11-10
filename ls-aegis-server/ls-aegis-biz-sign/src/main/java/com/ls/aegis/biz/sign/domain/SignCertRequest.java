package com.ls.aegis.biz.sign.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

import java.util.Date;

/**
 * 证书申请单。
 */
public class SignCertRequest extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    private Long requestId;
    private String requestType;
    private Long orgId;
    private Long projectId;
    private Long sealId;
    private String subjectName;
    private String subjectCode;
    private String status;
    private String approveBy;
    private Date approveTime;
    private String rejectReason;

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getSealId() { return sealId; }
    public void setSealId(Long sealId) { this.sealId = sealId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApproveBy() { return approveBy; }
    public void setApproveBy(String approveBy) { this.approveBy = approveBy; }
    public Date getApproveTime() { return approveTime; }
    public void setApproveTime(Date approveTime) { this.approveTime = approveTime; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
