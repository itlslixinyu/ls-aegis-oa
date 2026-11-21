package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "核销请求")
public class WriteOffReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotEmpty(message = "核销明细不能为空")
    @Valid
    private List<Item> items;
    @Length(max = 500)
    private String reason;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull(message = "开口ID不能为空")
        private Long openId;
        @NotNull(message = "核销金额不能为空")
        @Positive(message = "核销金额必须大于 0")
        private BigDecimal amount;
    }
}
