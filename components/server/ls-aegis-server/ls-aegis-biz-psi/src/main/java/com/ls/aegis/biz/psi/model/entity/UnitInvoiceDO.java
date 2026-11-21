package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.crypto.encryptor.Sm4Encryptor;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;

import java.io.Serial;

/**
 * 往来单位开票信息
 */
@Data
@TableName("psi_unit_invoice")
public class UnitInvoiceDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long unitId;
    private String title;
    private String taxNo;
    private String bankName;
    @FieldEncrypt(encryptor = Sm4Encryptor.class)
    private String accountNo;
    private String address;
    private String phone;
    /** 是否默认：0否 1是 */
    private Integer isDefault;
    private String remark;
}
