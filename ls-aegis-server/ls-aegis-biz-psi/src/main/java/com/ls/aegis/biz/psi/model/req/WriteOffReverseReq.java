package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "反核销请求")
public class WriteOffReverseReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Schema(description = "核销明细ID列表")
    private List<Long> writeOffItemIds;
    @Schema(description = "核销主表ID")
    private Long writeOffId;
    @Length(max = 500)
    private String reason;
}
