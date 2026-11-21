package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.ReceivableDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface PsiReceivableMapper extends PsiScopedMapper<ReceivableDO> {

    @DataPermission
    @Select("SELECT * FROM psi_receivable WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    ReceivableDO selectByIdForUpdate(@Param("id") Long id);
}
