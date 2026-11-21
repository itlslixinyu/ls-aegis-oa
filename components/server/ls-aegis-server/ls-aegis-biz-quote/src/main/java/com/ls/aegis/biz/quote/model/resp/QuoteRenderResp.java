package com.ls.aegis.biz.quote.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 报价单渲染结果（HTML 预览/打印）
 */
@Data
public class QuoteRenderResp {

    @Schema(description = "文档标题")
    private String title;

    @Schema(description = "渲染后的 HTML")
    private String html;
}
