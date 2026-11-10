package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "合同绑定项目")
public class BindProjectReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目 ID，空表示解除绑定 */
    @Schema(description = "项目ID，空则解绑")
    private Long projectId;
}
