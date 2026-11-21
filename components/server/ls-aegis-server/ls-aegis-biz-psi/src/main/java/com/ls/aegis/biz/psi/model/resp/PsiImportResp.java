package com.ls.aegis.biz.psi.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * PSI 导入执行结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "PSI 导入执行结果")
public class PsiImportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "处理总行数")
    private Integer totalRows;

    @Schema(description = "新增行数")
    private Integer insertRows;

    @Schema(description = "修改行数")
    private Integer updateRows;

    @Schema(description = "跳过行数")
    private Integer skipRows;
}
