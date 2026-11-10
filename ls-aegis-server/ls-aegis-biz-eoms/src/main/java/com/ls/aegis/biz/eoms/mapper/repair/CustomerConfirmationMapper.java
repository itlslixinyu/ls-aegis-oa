package com.ls.aegis.biz.eoms.mapper.repair;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import lombok.Data;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

/** Exact session ownership predicates replace operator department scope for customer H5. */
@Mapper
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface CustomerConfirmationMapper {
    String OWNED_ORDER = "SELECT id,tenant_id,customer_id,status FROM eoms_repair_order WHERE id=#{orderId} AND tenant_id=#{tenantId} AND customer_id=#{customerId} AND del_flag='0'";

    @Select(OWNED_ORDER + " FOR UPDATE")
    @Results(id = "ownedConfirmationOrder", value = {
        @Result(column = "id", property = "id"), @Result(column = "tenant_id", property = "tenantId"),
        @Result(column = "customer_id", property = "customerId"), @Result(column = "status", property = "status")
    })
    RepairOrder lockOwnedOrder(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId, @Param("orderId") Long orderId);

    @Select(OWNED_ORDER)
    @ResultMap("ownedConfirmationOrder")
    RepairOrder ownedOrder(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId, @Param("orderId") Long orderId);

    @Select("""
        SELECT e.related_id,e.operator,e.reason,e.create_time FROM eoms_finance_event e
        JOIN eoms_repair_order o ON o.id=e.aggregate_id
        WHERE e.aggregate_type='ORDER' AND e.action='CUSTOMER_CONFIRM'
        AND o.id=#{orderId} AND o.tenant_id=#{tenantId} AND o.customer_id=#{customerId} AND o.del_flag='0'
        """)
    @Results(value = {@Result(column = "related_id", property = "customerId"), @Result(column = "operator", property = "operator"),
        @Result(column = "reason", property = "reason"), @Result(column = "create_time", property = "confirmedAt")})
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    ConfirmationRow confirmation(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId, @Param("orderId") Long orderId);

    @Insert("""
        INSERT INTO eoms_finance_event(aggregate_type,aggregate_id,action,related_id,amount,operator,reason,create_time)
        SELECT 'ORDER',o.id,'CUSTOMER_CONFIRM',o.customer_id,NULL,#{operator},#{reason},CURRENT_TIMESTAMP
        FROM eoms_repair_order o WHERE o.id=#{orderId} AND o.tenant_id=#{tenantId} AND o.customer_id=#{customerId}
        AND o.del_flag='0' AND o.status IN ('WAIT_SETTLEMENT','FINISHED')
        """)
    int insertConfirmation(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId, @Param("orderId") Long orderId,
                           @Param("operator") String operator, @Param("reason") String reason);

    @Data
    class ConfirmationRow {
        private Long customerId;
        private String operator;
        private String reason;
        private LocalDateTime confirmedAt;
    }
}
