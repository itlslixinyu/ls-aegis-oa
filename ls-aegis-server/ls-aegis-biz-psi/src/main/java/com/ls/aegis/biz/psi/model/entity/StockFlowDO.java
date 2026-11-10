package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.StockFlowTypeEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水
 */
@Data
@TableName("psi_stock_flow")
public class StockFlowDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建人部门（数据权限） */
    private Long deptId;
    private Long warehouseId;
    private Long goodsId;
    private StockFlowTypeEnum flowType;
    private BigDecimal beforeQty;
    private BigDecimal changeQty;
    private BigDecimal afterQty;
    private BigDecimal beforeAmount;
    private BigDecimal changeAmount;
    private BigDecimal afterAmount;
    private BizSourceTypeEnum sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long confirmUser;
    private LocalDateTime confirmTime;
}
