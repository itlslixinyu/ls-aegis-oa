package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "合同请求")
public class ContractReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotBlank
    @Length(max = 200)
    private String name;
    @NotNull
    private LocalDateTime signDate;
    @NotNull
    private Long unitId;
    private Long projectId;
    @NotNull
    @DecimalMin(value = "0", message = "合同金额不能为负数")
    @Digits(integer = 16, fraction = 2, message = "合同金额最多16位整数、2位小数")
    private BigDecimal taxInclusiveAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    /** 附件文件 ID，多个以英文逗号分隔；传空串表示清空 */
    @Length(max = 500)
    private String fileId;
    @Length(max = 500)
    private String remark;
}
