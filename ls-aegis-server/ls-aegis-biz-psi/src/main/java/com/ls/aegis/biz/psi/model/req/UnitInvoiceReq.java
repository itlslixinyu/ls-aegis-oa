package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 往来单位开票信息请求
 */
@Data
@Schema(description = "往来单位开票信息请求")
public class UnitInvoiceReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "发票抬头", example = "示例科技有限公司")
    @NotBlank(message = "发票抬头不能为空")
    @Length(max = 128, message = "发票抬头长度不能超过 {max} 个字符")
    private String title;

    @Schema(description = "纳税人识别号")
    @Length(max = 64, message = "纳税人识别号长度不能超过 {max} 个字符")
    private String taxNo;

    @Schema(description = "开户银行")
    @Length(max = 128, message = "开户银行长度不能超过 {max} 个字符")
    private String bankName;

    @Schema(description = "银行账号")
    @Length(max = 64, message = "银行账号长度不能超过 {max} 个字符")
    private String accountNo;

    @Schema(description = "地址")
    @Length(max = 256, message = "地址长度不能超过 {max} 个字符")
    private String address;

    @Schema(description = "电话")
    @Length(max = 32, message = "电话长度不能超过 {max} 个字符")
    private String phone;

    @Schema(description = "是否默认：0否 1是", example = "0")
    private Integer isDefault;

    @Schema(description = "备注")
    @Length(max = 500, message = "备注长度不能超过 {max} 个字符")
    private String remark;
}
