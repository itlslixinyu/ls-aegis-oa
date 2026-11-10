package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.biz.psi.enums.PsiImportPolicyEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * PSI 主数据导入确认请求
 */
@Data
@Schema(description = "PSI 导入确认请求")
public class PsiImportReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "解析会话 Key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "导入会话不能为空")
    private String importKey;

    @Schema(description = "编码/名称已存在时的策略", example = "1")
    @NotNull(message = "重复策略不能为空")
    private PsiImportPolicyEnum duplicatePolicy;

    @Schema(description = "默认状态：1启用 2禁用（行内未填时使用）", example = "1")
    private Integer defaultStatus;
}
