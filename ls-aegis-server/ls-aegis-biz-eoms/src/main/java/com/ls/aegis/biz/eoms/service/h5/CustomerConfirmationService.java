package com.ls.aegis.biz.eoms.service.h5;

import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.mapper.repair.CustomerConfirmationMapper;
import com.ls.aegis.biz.eoms.model.resp.CustomerConfirmationResp;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.compat.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import com.ls.aegis.biz.eoms.domain.RepairApply;

@Service
@RequiredArgsConstructor
public class CustomerConfirmationService {
    private final CustomerConfirmationMapper mapper;
    private final IRepairOrderService orderService;
    @org.springframework.beans.factory.annotation.Autowired
    private H5CustomerScope customerScope;

    @Transactional(readOnly = true)
    public List<RepairApply> pendingConfirmations(com.ls.aegis.biz.eoms.domain.H5CustomerInfo session) {
        customerScope.requireIdentity(session);
        return mapper.pendingMemberConfirmations(session.getTenantId(),session.getCustomerId(),session.isCompanyAdmin() ? null : session.getAccountId());
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public CustomerConfirmationResp confirm(com.ls.aegis.biz.eoms.domain.H5CustomerInfo session, Long orderId, String remark) {
        customerScope.requireOrder(session, orderId);
        return confirmInternal(session.getTenantId(), session.getCustomerId(), orderId, remark,
            "CUSTOMER:" + session.getCustomerId() + ";MEMBER:" + session.getAccountId());
    }

    @Transactional(readOnly = true)
    public List<RepairApply> pendingConfirmations(Long tenantId, Long customerId) {
        if (tenantId == null || customerId == null) throw new ServiceException("客户会话无效或已过期");
        return mapper.pendingConfirmations(tenantId, customerId);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public CustomerConfirmationResp confirm(Long tenantId, Long customerId, Long orderId, String remark) {
        return confirmInternal(tenantId, customerId, orderId, remark, "CUSTOMER:" + customerId);
    }

    private CustomerConfirmationResp confirmInternal(Long tenantId, Long customerId, Long orderId, String remark, String operator) {
        requireIdentity(tenantId, customerId, orderId);
        String reason = remark == null || remark.isBlank() ? "客户确认维修完成" : remark.trim();
        if (reason.length() > 500) throw new ServiceException("确认备注不得超过500字");
        RepairOrder order = mapper.lockOwnedOrder(tenantId, customerId, orderId);
        requireOwned(order, tenantId, customerId, orderId);
        if (!Set.of("WAIT_SETTLEMENT", "FINISHED").contains(order.getStatus()))
            throw new ServiceException("仅待结算或已完成工单可确认维修结果");
        var existing = mapper.confirmation(tenantId, customerId, orderId);
        if (existing != null) return response(tenantId, customerId, orderId, existing);
        if (mapper.insertConfirmation(tenantId, customerId, orderId, operator, reason) != 1)
            throw new ServiceException("工单状态已变更，维修结果确认失败");
        var saved = mapper.confirmation(tenantId, customerId, orderId);
        if (saved == null) throw new ServiceException("维修结果确认未持久化");
        return response(tenantId, customerId, orderId, saved);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public CustomerConfirmationResp forCustomer(Long tenantId, Long customerId, Long orderId) {
        requireIdentity(tenantId, customerId, orderId);
        requireOwned(mapper.ownedOrder(tenantId, customerId, orderId), tenantId, customerId, orderId);
        return response(tenantId, customerId, orderId, mapper.confirmation(tenantId, customerId, orderId));
    }

    /** Staff visibility is authorized before reading the exact customer's event. */
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public CustomerConfirmationResp forStaff(Long orderId) {
        RepairOrder visible = orderService.selectRepairOrderById(orderId);
        if (visible == null) throw new ServiceException("工单不存在或无权访问");
        Long tenantId = UserContextHolder.getContext().getTenantId();
        tenantId = tenantId == null ? 0L : tenantId;
        if (!Objects.equals(tenantId, visible.getTenantId())) throw new ServiceException("工单租户不一致");
        return forCustomer(tenantId, visible.getCustomerId(), orderId);
    }

    private void requireIdentity(Long tenantId, Long customerId, Long orderId) {
        if (tenantId == null || tenantId < 0 || customerId == null || customerId <= 0 || orderId == null || orderId <= 0)
            throw new ServiceException("客户身份或工单标识无效，请重新登录");
    }
    private void requireOwned(RepairOrder order, Long tenantId, Long customerId, Long orderId) {
        if (order == null || !Objects.equals(orderId, order.getId()) || !Objects.equals(tenantId, order.getTenantId())
                || !Objects.equals(customerId, order.getCustomerId())) throw new ServiceException("工单不存在或无权访问");
    }
    private boolean validOperator(Long customerId, String operator) {
        return ("CUSTOMER:" + customerId).equals(operator) || operator != null && operator.matches("CUSTOMER:" + customerId + ";MEMBER:[1-9][0-9]*");
    }
    private CustomerConfirmationResp response(Long tenantId, Long customerId, Long orderId, CustomerConfirmationMapper.ConfirmationRow row) {
        var result = new CustomerConfirmationResp();result.setOrderId(orderId.toString());result.setCustomerId(customerId.toString());result.setConfirmed(row != null);
        if (row != null) {
            if (!Objects.equals(customerId, row.getCustomerId()) || !validOperator(customerId, row.getOperator())
                    || row.getConfirmedAt() == null || row.getReason() == null) throw new ServiceException("客户确认历史关联异常，请联系管理员核对");
            result.setConfirmedAt(row.getConfirmedAt());result.setReason(row.getReason());
        }
        return result;
    }
}
