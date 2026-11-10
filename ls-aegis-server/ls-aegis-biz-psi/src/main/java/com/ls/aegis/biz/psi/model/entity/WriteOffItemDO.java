package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.WriteOffDirectionEnum;
import com.ls.aegis.biz.psi.enums.WriteOffOpenTypeEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@TableName("psi_write_off_item")
public class WriteOffItemDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long writeOffId;
    private Long receiptId;
    private Long paymentId;
    private WriteOffOpenTypeEnum openType;
    private Long openId;
    private WriteOffDirectionEnum direction;
    private BigDecimal amount;
    private String reason;
    private Long cashSequence;
}
