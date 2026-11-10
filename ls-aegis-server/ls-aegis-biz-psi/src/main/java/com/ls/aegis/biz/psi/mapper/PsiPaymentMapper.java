package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.PaymentDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface PsiPaymentMapper extends PsiScopedMapper<PaymentDO> {

    @DataPermission
    @Select("SELECT * FROM psi_payment WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    PaymentDO selectByIdForUpdate(@Param("id") Long id);
}
