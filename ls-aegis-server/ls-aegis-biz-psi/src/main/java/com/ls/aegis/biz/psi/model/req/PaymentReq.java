package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "付款请求")
public class PaymentReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(message = "付款日期不能为空")
    private LocalDateTime paymentDate;
    @NotNull(message = "供应商不能为空")
    private Long unitId;
    private Long accountId;
    private String payMethod;
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于 0")
    private BigDecimal amount;
    @Length(max = 500)
    private String remark;
}
