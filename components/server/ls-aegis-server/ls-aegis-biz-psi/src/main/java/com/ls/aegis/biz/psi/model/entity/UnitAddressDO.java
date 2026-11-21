package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;

/**
 * 往来单位收货地址
 */
@Data
@TableName("psi_unit_address")
public class UnitAddressDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long unitId;
    private String contactName;
    private String mobile;
    private String region;
    private String address;
    /** 是否默认：0否 1是 */
    private Integer isDefault;
    private String remark;
}
