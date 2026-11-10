package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.UnitDO;

import org.apache.ibatis.annotations.Mapper;

/**
 * 往来单位 Mapper
 */
@Mapper
public interface UnitMapper extends PsiScopedMapper<UnitDO> {
    /** Capability resolution after invitation verification; explicit tenant+ID cannot be omitted. */
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    @org.apache.ibatis.annotations.Select("SELECT * FROM psi_unit WHERE tenant_id = #{tenantId} AND id = #{customerId} AND deleted = 0 AND status = 1 AND unit_type IN ('CUSTOMER','BOTH')")
    UnitDO selectH5Customer(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                           @org.apache.ibatis.annotations.Param("customerId") Long customerId);

}
