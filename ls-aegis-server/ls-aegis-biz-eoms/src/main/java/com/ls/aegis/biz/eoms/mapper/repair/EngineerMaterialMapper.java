package com.ls.aegis.biz.eoms.mapper.repair;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ls.aegis.biz.eoms.domain.h5.MaterialCatalogItem;
import com.ls.aegis.biz.eoms.domain.h5.EngineerMaterialView;
/** Caller first authorizes the exact assigned task. All SQL additionally binds that task tenant. */
public interface EngineerMaterialMapper {
    @InterceptorIgnore(tenantLine="true")
    List<MaterialCatalogItem> goods(@Param("tenant") Long tenant,@Param("keyword") String keyword);
    @InterceptorIgnore(tenantLine="true")
    List<MaterialCatalogItem> warehouses(@Param("tenant") Long tenant);
    @InterceptorIgnore(tenantLine="true")
    MaterialCatalogItem good(@Param("tenant") Long tenant,@Param("id") Long id);
    @InterceptorIgnore(tenantLine="true")
    List<EngineerMaterialView> materials(@Param("tenant") Long tenant,@Param("orderId") Long orderId);
}
