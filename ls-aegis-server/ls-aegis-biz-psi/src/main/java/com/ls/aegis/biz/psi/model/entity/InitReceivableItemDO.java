package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@TableName("psi_init_receivable_item")
public class InitReceivableItemDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long initId;
    private Long unitId;
    private BigDecimal taxInclusiveAmount;
    private Integer sort;
}
