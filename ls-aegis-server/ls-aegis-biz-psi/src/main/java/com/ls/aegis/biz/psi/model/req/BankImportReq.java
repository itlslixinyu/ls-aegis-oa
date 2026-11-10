package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.biz.psi.enums.BankDirectionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import org.hibernate.validator.constraints.Length;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "银行流水导入")
public class BankImportReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull
    private Long accountId;
    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private LocalDateTime stmtDate;
        @NotNull
        private BankDirectionEnum direction;
        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 16, fraction = 2)
        private BigDecimal amount;
        @Length(max = 128)
        private String counterparty;
        @Length(max = 256)
        private String summary;
        @Length(max = 64)
        private String bankSeqNo;
    }
}
