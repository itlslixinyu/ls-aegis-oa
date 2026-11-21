package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.InitReceivableDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface InitReceivableMapper extends PsiScopedMapper<InitReceivableDO> {

    @DataPermission
    @Select("SELECT * FROM psi_init_receivable WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    InitReceivableDO selectByIdForUpdate(@Param("id") Long id);
}
