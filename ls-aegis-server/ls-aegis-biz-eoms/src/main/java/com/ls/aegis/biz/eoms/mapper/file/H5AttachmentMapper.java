package com.ls.aegis.biz.eoms.mapper.file;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** Internal H5 operations use the authenticated customer identity, never operator data scope. */
public interface H5AttachmentMapper {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT dept_id FROM psi_unit WHERE id=#{customerId} AND tenant_id=#{tenantId} AND deleted=0 AND status=1 AND unit_type IN ('CUSTOMER','BOTH')")
    Long selectActiveCustomerDept(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Update("UPDATE sys_file SET metadata=#{metadata} WHERE id=#{id} AND deleted=0 AND (metadata=#{previous,jdbcType=VARCHAR} OR (metadata IS NULL AND #{previous,jdbcType=VARCHAR} IS NULL))")
    int bindUpload(@Param("id") Long id, @Param("previous") String previous, @Param("metadata") String metadata);
}
