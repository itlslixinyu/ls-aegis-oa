package com.ls.aegis.biz.psi.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ls.aegis.biz.psi.model.entity.StockOutDO;
import com.ls.aegis.biz.psi.model.entity.StockOutItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** Only the EOMS-authorized, locked repair order may call this internal cross-domain path. No HTTP CRUD usage. */
@Mapper
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface RepairStockOutInternalMapper {
    List<StockOutDO> lockRepairHeads(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId, @Param("prefix") String prefix);
    List<StockOutItemDO> selectItems(@Param("tenantId") Long tenantId, @Param("outId") Long outId);
    int markVoid(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
