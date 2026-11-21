package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.StockOutDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface StockOutMapper extends PsiScopedMapper<StockOutDO> {

    @DataPermission
    @Select("SELECT * FROM psi_stock_out WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    StockOutDO selectByIdForUpdate(@Param("id") Long id);
}
