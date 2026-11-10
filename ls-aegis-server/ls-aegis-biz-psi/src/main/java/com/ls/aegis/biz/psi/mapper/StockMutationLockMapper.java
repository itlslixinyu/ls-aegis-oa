package com.ls.aegis.biz.psi.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Internal serialization anchors; tenant is always an explicit part of the composite primary key. */
@Mapper
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface StockMutationLockMapper {
    int ensureAnchor(@Param("tenantId") Long tenantId, @Param("warehouseId") Long warehouseId, @Param("goodsId") Long goodsId);
    Long lockAnchor(@Param("tenantId") Long tenantId, @Param("warehouseId") Long warehouseId, @Param("goodsId") Long goodsId);
}
