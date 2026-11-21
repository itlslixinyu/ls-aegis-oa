package com.ls.aegis.biz.psi.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 打印渲染结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "打印渲染结果")
public class PrintRenderResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "打印标题")
    private String title;

    @Schema(description = "渲染后的 HTML")
    private String html;

    @Schema(description = "Word/附件模板文件 ID，可空")
    private Long fileId;
}
