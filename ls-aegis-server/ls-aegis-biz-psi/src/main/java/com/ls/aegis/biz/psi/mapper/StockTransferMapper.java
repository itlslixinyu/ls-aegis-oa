package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.StockTransferDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface StockTransferMapper extends PsiScopedMapper<StockTransferDO> {

    @DataPermission
    @Select("SELECT * FROM psi_stock_transfer WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    StockTransferDO selectByIdForUpdate(@Param("id") Long id);
}
