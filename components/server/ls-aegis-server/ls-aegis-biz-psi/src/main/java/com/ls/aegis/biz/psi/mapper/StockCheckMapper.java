package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.StockCheckDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface StockCheckMapper extends PsiScopedMapper<StockCheckDO> {

    @DataPermission
    @Select("SELECT * FROM psi_stock_check WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    StockCheckDO selectByIdForUpdate(@Param("id") Long id);
}
