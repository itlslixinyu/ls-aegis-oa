package com.ls.aegis.biz.eoms.domain.h5;
import lombok.Data;
import java.math.BigDecimal;
@Data public class EngineerMaterialView {
    private String id;
    private String materialName;
    private String specification;
    private String warehouseName;
    private BigDecimal quantity;
    private String remark;
}
