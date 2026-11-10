package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.StockOutBatchDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.data.mapper.BaseMapper;

import java.util.List;

@Mapper
public interface StockOutBatchMapper extends BaseMapper<StockOutBatchDO> {

    @Select("""
        SELECT * FROM psi_stock_out_batch
        WHERE stock_out_item_id = #{stockOutItemId} AND deleted = 0
        ORDER BY id DESC
        """)
    List<StockOutBatchDO> selectByOutItemDesc(@Param("stockOutItemId") Long stockOutItemId);
}
