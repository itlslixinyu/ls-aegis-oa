package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.UnitNatureEnum;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;

/**
 * 往来单位
 */
@Data
@TableName("psi_unit")
public class UnitDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建人部门（数据权限） */
    private Long deptId;
    private String unitCode;
    private String unitName;
    private UnitTypeEnum unitType;
    private UnitNatureEnum unitNature;
    private String creditCode;
    private String address;
    private Long categoryId;
    private String levelCode;
    private DisEnableStatusEnum status;
    private String remark;
}
