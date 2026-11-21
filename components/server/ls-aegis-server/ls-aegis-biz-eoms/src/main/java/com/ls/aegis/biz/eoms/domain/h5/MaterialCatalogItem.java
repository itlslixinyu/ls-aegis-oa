package com.ls.aegis.biz.eoms.domain.h5;
import lombok.Data;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;
/** Shared option shape; IDs are strings and financial reference values never leave the server. */
@Data public class MaterialCatalogItem {
    private String id;
    private String name;
    private String spec;
    private String unit;
    @JsonIgnore private BigDecimal salePrice;
    @JsonIgnore private BigDecimal purchasePrice;
}
