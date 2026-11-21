package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "银行对账匹配")
public class BankMatchReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull
    private Long statementId;
    /** RECEIPT / PAYMENT */
    @NotBlank
    private String matchBizType;
    @NotNull
    private Long matchBizId;
}
