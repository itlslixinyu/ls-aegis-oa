package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.InitPrepayDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface InitPrepayMapper extends PsiScopedMapper<InitPrepayDO> {

    @DataPermission
    @Select("SELECT * FROM psi_init_prepay WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    InitPrepayDO selectByIdForUpdate(@Param("id") Long id);
}
