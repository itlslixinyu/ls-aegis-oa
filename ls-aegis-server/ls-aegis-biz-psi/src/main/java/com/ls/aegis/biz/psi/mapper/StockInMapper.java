package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.StockInDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface StockInMapper extends PsiScopedMapper<StockInDO> {

    @DataPermission
    @Select("SELECT * FROM psi_stock_in WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    StockInDO selectByIdForUpdate(@Param("id") Long id);
}
