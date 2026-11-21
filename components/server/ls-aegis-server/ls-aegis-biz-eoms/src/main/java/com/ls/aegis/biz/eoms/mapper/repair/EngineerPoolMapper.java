package com.ls.aegis.biz.eoms.mapper.repair;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ls.aegis.biz.eoms.domain.RepairApply;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.domain.h5.EngineerPoolItem;

/** All statements explicitly bind the authenticated tenant; pool visibility is tenant-wide. */
public interface EngineerPoolMapper {
    @InterceptorIgnore(tenantLine = "true")
    List<EngineerPoolItem> list(@Param("tenantId") Long tenantId, @Param("cutoff") Date cutoff);
    @InterceptorIgnore(tenantLine = "true")
    RepairApply lockApply(@Param("tenantId") Long tenantId, @Param("id") Long id);
    @InterceptorIgnore(tenantLine = "true")
    List<RepairOrder> lockOrders(@Param("tenantId") Long tenantId, @Param("applyId") Long applyId);
    @InterceptorIgnore(tenantLine = "true")
    int markConverted(@Param("tenantId") Long tenantId, @Param("id") Long id,
                      @Param("expected") String expected, @Param("actor") String actor);
}
