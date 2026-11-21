package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.crypto.encryptor.Sm4Encryptor;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;

import java.io.Serial;

@Data
@TableName("psi_company_account")
public class CompanyAccountDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 创建人部门（数据权限） */
    private Long deptId;
    private String accountName;
    private String bankName;
    /** 库内 SM4；唯一索引建在密文上（确定性加密） */
    @FieldEncrypt(encryptor = Sm4Encryptor.class)
    private String accountNo;
    private DisEnableStatusEnum status;
    private String remark;
}
