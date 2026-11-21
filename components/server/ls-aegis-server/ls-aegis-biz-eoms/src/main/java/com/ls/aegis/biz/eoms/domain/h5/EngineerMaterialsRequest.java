package com.ls.aegis.biz.eoms.domain.h5;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.h5.EngineerRecordRequest.MaterialLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Independent optional materials entry, using the same validated line contract. */
@Data
public class EngineerMaterialsRequest {
    @NotNull @Pattern(regexp="[A-Za-z0-9_-]{16,80}", message="提交标识无效")
    private String requestId;
    @Valid @NotEmpty(message="配件不能为空") @Size(max=20, message="每次最多登记20项配件")
    private List<@NotNull MaterialLine> materials;
}
