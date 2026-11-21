package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.PayableDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface PayableMapper extends PsiScopedMapper<PayableDO> {

    @DataPermission
    @Select("SELECT * FROM psi_payable WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    PayableDO selectByIdForUpdate(@Param("id") Long id);
}
