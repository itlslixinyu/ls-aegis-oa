package com.ls.aegis.biz.psi.model.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/** 登记已实际发生的退货退款，生成与原资金单/退货一一可追溯的反向资金单。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CashRefundReq extends CashActionReq {
    @NotNull(message = "退货单不能为空")
    private Long stockReturnId;
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于零")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal amount;
}
