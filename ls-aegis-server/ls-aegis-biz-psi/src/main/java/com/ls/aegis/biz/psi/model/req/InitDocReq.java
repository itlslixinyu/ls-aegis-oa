package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.biz.psi.enums.InitTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "期初单据请求")
public class InitDocReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull
    private InitTypeEnum type;
    @NotNull
    private LocalDateTime initDate;
    private Long warehouseId;
    @Length(max = 500)
    private String remark;
    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long goodsId;
        private BigDecimal qty;
        private BigDecimal untaxPrice;
        private Long unitId;
        private BigDecimal taxInclusiveAmount;
    }
}
