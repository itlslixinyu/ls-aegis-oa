package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "银行账户请求")
public class CompanyAccountReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long tenantId;
    /** 创建人部门（数据权限，服务端填充） */
    private Long deptId;
    @NotBlank
    @Length(max = 128)
    private String accountName;
    @NotBlank
    @Length(max = 128)
    private String bankName;
    @NotBlank
    @Length(max = 64)
    private String accountNo;
    @NotNull
    private DisEnableStatusEnum status;
    @Length(max = 500)
    private String remark;
}
