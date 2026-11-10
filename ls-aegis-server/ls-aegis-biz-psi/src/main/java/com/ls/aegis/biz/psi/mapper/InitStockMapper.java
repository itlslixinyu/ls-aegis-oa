package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.InitStockDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface InitStockMapper extends PsiScopedMapper<InitStockDO> {

    @DataPermission
    @Select("SELECT * FROM psi_init_stock WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    InitStockDO selectByIdForUpdate(@Param("id") Long id);
}
