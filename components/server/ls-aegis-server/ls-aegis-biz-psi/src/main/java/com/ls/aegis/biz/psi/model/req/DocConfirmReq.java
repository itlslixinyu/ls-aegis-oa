package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 单据确认请求
 */
@Data
@Schema(description = "单据确认请求")
public class DocConfirmReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "幂等键")
    @NotBlank(message = "幂等键不能为空")
    @Length(max = 64, message = "幂等键长度不能超过64")
    private String idempotentKey;

    /**
     * 盘点确认时用于乐观锁：须与单据上的 snapshotVersion 一致
     */
    @Schema(description = "库存快照版本（盘点确认校验）")
    private Long snapshotVersion;

    /**
     * 收付款确认时可选即时核销明细（文档 §2.5）；其它单据忽略
     */
    @Schema(description = "可选即时核销明细（回款/付款确认）")
    @Valid
    private List<WriteOffReq.Item> writeOffItems;

    @Schema(description = "即时核销备注")
    @Length(max = 500)
    private String writeOffReason;
}
