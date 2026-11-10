package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("psi_contract_sale")
public class ContractSaleDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private String contractNo;
    /** 库列名为 contract_name */
    @TableField("contract_name")
    private String name;
    private LocalDateTime signDate;
    private Long unitId;
    /** 所属项目（sign_project.project_id） */
    private Long projectId;
    /** 创建人部门（数据权限） */
    private Long deptId;
    private BigDecimal taxInclusiveAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private DocStatusEnum docStatus;
    /** 附件文件 ID，多个以英文逗号分隔 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fileId;
    private String remark;
    private Long confirmUser;
    private LocalDateTime confirmTime;
}
