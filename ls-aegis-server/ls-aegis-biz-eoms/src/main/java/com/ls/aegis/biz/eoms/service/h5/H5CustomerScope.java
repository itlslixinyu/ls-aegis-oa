package com.ls.aegis.biz.eoms.service.h5;

import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.mapper.repair.*;
import com.ls.aegis.compat.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Objects;

/** Member scope is enforced with server identity, independently of hidden UI controls. */
@Service
@RequiredArgsConstructor
public class H5CustomerScope {
    private final RepairApplyMapper applies;
    private final RepairOrderMapper orders;
    public void requireIdentity(H5CustomerInfo session) {
        if (session == null || session.getTenantId() == null || session.getCustomerId() == null || session.getAccountId() == null
            || !java.util.Set.of("ADMIN","MEMBER").contains(session.getAccountRole())) throw new ServiceException("成员会话无效，请重新登录");
    }
    public RepairApply requireApply(H5CustomerInfo session, Long id) {
        requireIdentity(session); var apply = id == null ? null : applies.selectRepairApplyById(id);
        if (apply == null || !Objects.equals(session.getTenantId(),apply.getTenantId()) || !Objects.equals(session.getCustomerId(),apply.getCustomerId())
            || !(session.isCompanyAdmin() || Objects.equals(session.getAccountId(),apply.getReporterAccountId())))
            throw new ServiceException("报修记录不存在或无权查看");
        return apply;
    }
    public RepairOrder requireOrder(H5CustomerInfo session, Long id) {
        requireIdentity(session); var order = id == null ? null : orders.selectRepairOrderById(id);
        if (order == null || !Objects.equals(session.getTenantId(),order.getTenantId()) || !Objects.equals(session.getCustomerId(),order.getCustomerId()))
            throw new ServiceException("工单不存在或无权访问");
        if (!session.isCompanyAdmin()) requireApply(session,order.getApplyId());
        return order;
    }
}
