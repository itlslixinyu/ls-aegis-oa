package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@TableName("psi_init_prepay_item")
public class InitPrepayItemDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long initId;
    private Long unitId;
    /** 历史列名为 amount；与应收/应付明细统一为含税金额语义 */
    @TableField("amount")
    private BigDecimal taxInclusiveAmount;
    private Integer sort;
}
