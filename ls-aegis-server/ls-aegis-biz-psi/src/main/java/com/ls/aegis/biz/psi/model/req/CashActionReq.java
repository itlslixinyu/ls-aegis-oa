package com.ls.aegis.biz.psi.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 台账更正必须有原因；不会自动发起银行付款。 */
@Data
public class CashActionReq {
    @NotBlank(message = "原因不能为空")
    @Size(max = 500)
    private String reason;
    @NotBlank(message = "幂等键不能为空")
    @Size(max = 64)
    private String idempotentKey;
}
