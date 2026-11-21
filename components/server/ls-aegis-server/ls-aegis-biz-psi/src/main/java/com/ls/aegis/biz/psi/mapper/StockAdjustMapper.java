package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.StockAdjustDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface StockAdjustMapper extends PsiScopedMapper<StockAdjustDO> {

    @DataPermission
    @Select("SELECT * FROM psi_stock_adjust WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    StockAdjustDO selectByIdForUpdate(@Param("id") Long id);
}
