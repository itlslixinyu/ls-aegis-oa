package com.ls.aegis.biz.eoms.domain.h5;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.ls.aegis.biz.eoms.domain.RepairRecord;

/** Record and optional materials are committed together; no client-supplied financial fields. */
public class EngineerRecordRequest extends RepairRecord {
    @Valid @Size(max=20, message="每次最多登记20项配件")
    private List<@NotNull MaterialLine> materials = new ArrayList<>();
    public List<MaterialLine> getMaterials(){return materials;}
    public void setMaterials(List<MaterialLine> value){materials=value;}
    @Override @NotBlank @Size(max=500) public String getFaultReason(){return super.getFaultReason();}
    @Override @NotBlank @Size(max=1000) public String getCheckProcess(){return super.getCheckProcess();}
    @Override @NotBlank @Size(max=1000) public String getSolution(){return super.getSolution();}
    @Override @NotBlank @Size(max=500) public String getResult(){return super.getResult();}
    @Data public static class MaterialLine {
        @NotNull @Positive private Long materialId;
        @NotNull @Positive private Long warehouseId;
        @NotNull @DecimalMin("0.01") @Digits(integer=8, fraction=2) private BigDecimal quantity;
        @Size(max=500) private String remark;
    }
}
