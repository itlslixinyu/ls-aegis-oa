package com.ls.aegis.biz.psi.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * PSI 导入解析结果
 */
@Data
@Schema(description = "PSI 导入解析结果")
public class PsiImportParseResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "导入会话 Key")
    private String importKey;

    @Schema(description = "总计行数")
    private Integer totalRows;

    @Schema(description = "有效行数")
    private Integer validRows;

    @Schema(description = "库中已存在行数（按编码或名称）")
    private Integer duplicateRows;
}
