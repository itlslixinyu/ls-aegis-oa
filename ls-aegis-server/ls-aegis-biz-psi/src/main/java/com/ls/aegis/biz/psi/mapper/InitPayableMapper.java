package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.InitPayableDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface InitPayableMapper extends PsiScopedMapper<InitPayableDO> {

    @DataPermission
    @Select("SELECT * FROM psi_init_payable WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    InitPayableDO selectByIdForUpdate(@Param("id") Long id);
}
