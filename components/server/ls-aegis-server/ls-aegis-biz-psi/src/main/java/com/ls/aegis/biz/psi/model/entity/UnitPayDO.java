package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.crypto.encryptor.Sm4Encryptor;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;

import java.io.Serial;

/**
 * 往来单位付款/收款账户
 */
@Data
@TableName("psi_unit_pay")
public class UnitPayDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long unitId;
    private String accountName;
    private String bankName;
    @FieldEncrypt(encryptor = Sm4Encryptor.class)
    private String accountNo;
    /** 是否默认：0否 1是 */
    private Integer isDefault;
    private String remark;
}
