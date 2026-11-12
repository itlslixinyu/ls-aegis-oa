package com.ls.aegis.biz.eoms.service.h5;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import cn.dev33.satoken.stp.StpUtil;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.common.config.TenantExtensionProperties;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.domain.h5.EngineerPoolItem;
import com.ls.aegis.biz.eoms.mapper.repair.EngineerPoolMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import top.continew.starter.extension.tenant.context.TenantContextHolder;

/** Eligibility is time-derived: no scheduled writes and no dependency on manual confirmation. */
@Service
@RequiredArgsConstructor
public class EngineerPoolService {
    static final long WAIT_MILLIS = Duration.ofMinutes(10).toMillis();
    private final EngineerPoolMapper poolMapper;
    private final RepairOrderMapper orderMapper;
    private final EomsNoGenerator numbers;
    private final TenantExtensionProperties tenants;

    public List<EngineerPoolItem> list(Long tenant) {
        return poolMapper.list(tenant, new Date(System.currentTimeMillis() - WAIT_MILLIS));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long claim(Long applyId) {
        Long tenant = authorizeEngineer();
        if (applyId == null || applyId <= 0) throw new ServiceException("报修编号无效");
        var apply = poolMapper.lockApply(tenant, applyId);
        if (apply == null) throw new ServiceException("报修不存在或无权领取");
        var orders = poolMapper.lockOrders(tenant, applyId);
        if (orders.size() > 1) throw new ServiceException("报修存在多个工单，请联系管理员核对");
        Long actorId = UserContextHolder.getUserId();
        String actor = UserContextHolder.getUsername();
        if (!orders.isEmpty()) {
            var existing = orders.get(0);
            // A retry of the winning claim returns its existing task without another write.
            if ("ACCEPTED".equals(existing.getStatus()) && Objects.equals(actorId, existing.getEngineerId())) return existing.getId();
            if (!"WAIT_ACCEPT".equals(existing.getStatus()) || existing.getAcceptTime() != null)
                throw new ServiceException("该报修已被接单，请刷新工单池");
        }
        if (apply.getApplyTime() == null || System.currentTimeMillis() - apply.getApplyTime().getTime() < WAIT_MILLIS)
            throw new ServiceException("该报修尚未达到十分钟开放领取时间");
        if (apply.getStatus() == null || !List.of("WAIT", "CONFIRM", "ORDER").contains(apply.getStatus())
            || ("ORDER".equals(apply.getStatus()) && orders.isEmpty()))
            throw new ServiceException("报修状态已变更，请刷新工单池");
        RepairOrder order = orders.isEmpty() ? new RepairOrder() : orders.get(0);
        boolean create = orders.isEmpty();
        if (create) {
            order.setOrderNo(numbers.generateOrderNo());
            order.setApplyId(applyId); order.setCustomerId(apply.getCustomerId());
            order.setPhotos(apply.getPhotos()); order.setDeptId(apply.getDeptId()); order.setTenantId(tenant);
            order.setDelFlag("0"); order.setCreateBy(actor); order.setCreateUser(actorId);
            EomsDataScopeHelper.fillOnInsert(order);
        }
        order.setEngineerId(actorId); order.setStatus("ACCEPTED");
        order.setAcceptTime(new Date()); order.setUpdateBy(actor);
        int changed = create ? orderMapper.insertRepairOrder(order) : orderMapper.updateRepairOrder(order);
        if (changed != 1 || order.getId() == null) throw new ServiceException("领取失败，请刷新后重试");
        if (!"ORDER".equals(apply.getStatus()) && poolMapper.markConverted(tenant, applyId, apply.getStatus(), actor) != 1)
            throw new ServiceException("报修状态已变更，领取未完成");
        return order.getId();
    }

    public Long authorizeEngineer() {
        StpUtil.checkLogin();
        var context = UserContextHolder.getContext();
        if (context == null || context.getId() == null) throw new ServiceException("工程师身份无效");
        Long tenant = TenantContextHolder.isTenantEnabled() ? context.getTenantId() : tenants.getDefaultTenantId();
        if (tenant == null || orderMapper.countEnabledEngineer(tenant, context.getId()) != 1)
            throw new ServiceException("仅本租户启用的工程师可使用工单池");
        return tenant;
    }
}
