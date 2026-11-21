package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.StockReturnDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

import java.math.BigDecimal;

@Mapper
public interface StockReturnMapper extends PsiScopedMapper<StockReturnDO> {

    @DataPermission
    @Select("SELECT * FROM psi_stock_return WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    StockReturnDO selectByIdForUpdate(@Param("id") Long id);

    @Select("""
        SELECT COALESCE(SUM(i.quantity), 0)
        FROM psi_stock_return_item i
        INNER JOIN psi_stock_return r ON r.id = i.return_id AND r.deleted = 0
        WHERE i.deleted = 0
          AND r.doc_status = 'CONFIRMED'
          AND i.origin_item_id = #{originItemId}
        """)
    BigDecimal sumReturnedQty(@Param("originItemId") Long originItemId);
}
