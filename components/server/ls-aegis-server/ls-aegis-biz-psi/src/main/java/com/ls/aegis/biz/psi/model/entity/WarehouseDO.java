package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;

/**
 * 仓库
 */
@Data
@TableName("psi_warehouse")
public class WarehouseDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建人部门（数据权限） */
    private Long deptId;
    private String warehouseCode;
    private String warehouseName;
    /** 负责人用户 ID（sys_user.id） */
    private Long managerUserId;
    /** 负责人姓名（冗余展示） */
    private String manager;
    private String address;
    private Boolean isDefault;
    private DisEnableStatusEnum status;
    private String remark;
}
