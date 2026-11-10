package com.ls.aegis.biz.eoms.service.impl.repair;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.SecurityUtils;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.domain.RepairSettlement;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairRecordMapper;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.biz.eoms.service.repair.IRepairSettlementService;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import com.ls.aegis.biz.eoms.utils.EomsPlatformFileHelper;
import com.ls.aegis.biz.eoms.utils.GeoUtils;

/**
 * RepairOrder Service实现
 *
 * @author EOMS Generator
 */
@Service
public class RepairOrderServiceImpl implements IRepairOrderService
{
    @Autowired
    private RepairOrderMapper repairOrderMapper;
    @Autowired
    private com.ls.aegis.biz.eoms.mapper.repair.RepairSettlementMapper cancelSettlementMapper;
    @Autowired
    private com.ls.aegis.common.api.psi.RepairStockOutApi cancelStockApi;
    @Autowired
    private com.ls.aegis.biz.eoms.mapper.finance.FinanceEventMapper cancelEventMapper;

    @Autowired
    private com.ls.aegis.biz.eoms.mapper.repair.RepairApplyMapper repairApplyMapper;

    @Autowired
    private RepairRecordMapper repairRecordMapper;

    @Autowired
    private EomsNoGenerator eomsNoGenerator;

    @Autowired
    @Lazy
    private IRepairSettlementService repairSettlementService;

    @Autowired
    private GeoUtils geoUtils;

    @Autowired
    private EomsPlatformFileHelper platformFileHelper;

    @Override
    public RepairOrder selectRepairOrderById(Long id)
    {
        RepairOrder order;
        if (EomsDataScopeHelper.shouldFilter())
        {
            // 详情与列表共用 DataScope，避免越权按 ID 直查
            RepairOrder query = new RepairOrder();
            query.setId(id);
            EomsDataScopeHelper.apply(query, true, "o");
            List<RepairOrder> list = repairOrderMapper.selectRepairOrderList(query);
            order = list.isEmpty() ? null : list.get(0);
        }
        else
        {
            order = repairOrderMapper.selectRepairOrderById(id);
        }
        platformFileHelper.fillOrder(order);
        return order;
    }

    @Override
    public List<RepairOrder> selectRepairOrderList(RepairOrder repairOrder)
    {
        EomsDataScopeHelper.apply(repairOrder, true, "o");
        List<RepairOrder> list = repairOrderMapper.selectRepairOrderList(repairOrder);
        platformFileHelper.fillOrders(list);
        return list;
    }

    @Override
    public List<Map<String, Object>> selectEngineerOptions()
    {
        return repairOrderMapper.selectEngineerOptions();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRepairOrder(RepairOrder repairOrder)
    {
        // 自动生成工单编号
        if (StringUtils.isEmpty(repairOrder.getOrderNo()))
        {
            repairOrder.setOrderNo(eomsNoGenerator.generateOrderNo());
        }
        // 默认状态：待接单
        repairOrder.setStatus(EomsConstants.ORDER_STATUS_WAIT_ACCEPT);
        // 默认删除标志
        repairOrder.setDelFlag("0");
        EomsDataScopeHelper.fillOnInsert(repairOrder);
        return repairOrderMapper.insertRepairOrder(repairOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRepairOrder(RepairOrder repairOrder)
    {
        if (repairOrder == null || selectRepairOrderById(repairOrder.getId()) == null)
            throw new ServiceException("工单不存在或无权访问");
        RepairOrder existing = repairOrderMapper.lockOrder(repairOrder.getId());
        if (existing == null || java.util.Set.of("FINISHED", "CLOSED").contains(existing.getStatus()))
            throw new ServiceException("已结算或关闭工单不可直接编辑，请先按正式业务流程处理");
        if (repairOrder.getStatus() != null && !repairOrder.getStatus().equals(existing.getStatus()))
            throw new ServiceException("状态变更请使用专用业务接口", EomsConstants.CODE_STATUS_ILLEGAL);
        repairOrder.setStatus(existing.getStatus());
        repairOrder.setDelFlag("0");
        repairOrder.setApplyId(existing.getApplyId());
        repairOrder.setQuoteId(existing.getQuoteId());
        repairOrder.setCustomerId(existing.getCustomerId());
        repairOrder.setEngineerId(existing.getEngineerId());
        repairOrder.setFinishTime(existing.getFinishTime());
        return repairOrderMapper.updateRepairOrder(repairOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRepairOrderByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        java.util.List<RepairOrder> orders = new java.util.ArrayList<>();
        for (Long id : java.util.Arrays.stream(ids).distinct().sorted().toList()) {
            RepairOrder visible = selectRepairOrderById(id);
            if (visible == null) throw new ServiceException("工单不存在或无权访问");
            orders.add(visible);
        }
        // Apply→order lock order matches toOrder, preventing concurrent reassignment/deletion.
        for (Long applyId : orders.stream().map(RepairOrder::getApplyId).filter(java.util.Objects::nonNull).distinct().sorted().toList())
            repairApplyMapper.lockApply(applyId);
        for (RepairOrder visible : orders) {
            RepairOrder order = repairOrderMapper.lockOrder(visible.getId());
            if (order == null || !"WAIT_ACCEPT".equals(order.getStatus())) throw new ServiceException("仅未接单工单可删除；已执行工单请使用正式业务撤销流程");
            if (repairRecordMapper.countByRepairOrderId(order.getId()) != 0
                    || repairSettlementService.selectSettlementByOrderId(order.getId()) != null)
                throw new ServiceException("工单已有维修记录或结算，不允许删除");
        }
        int rows = repairOrderMapper.deleteRepairOrderByIds(ids);
        for (RepairOrder order : orders) if (order.getApplyId() != null) repairApplyMapper.restoreAfterOrderDelete(order.getApplyId());
        // Photos are shared with the originating apply; deleting order must retain platform files.
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRepairOrderById(Long id)
    {
        return deleteRepairOrderByIds(new Long[]{id});
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int assignEngineer(Long orderId, Long engineerId, String remark)
    {
        if (engineerId == null)
        {
            throw new ServiceException("工程师ID不能为空", EomsConstants.CODE_STATUS_ILLEGAL);
        }
        RepairOrder before = mustExist(orderId);
        String oldStatus = before.getStatus();
        // 仅待接单状态可派单
        if (!EomsConstants.ORDER_STATUS_WAIT_ACCEPT.equals(oldStatus))
        {
            throw new ServiceException("当前状态[" + oldStatus + "]不可派单，仅待接单状态可派单",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }
        if (repairOrderMapper.countEnabledEngineer(before.getTenantId(), engineerId) != 1)
            throw new ServiceException("目标用户不是本租户启用的工程师，不能派单", EomsConstants.CODE_STATUS_ILLEGAL);
        RepairOrder after = new RepairOrder();
        after.setId(orderId);
        after.setEngineerId(engineerId);
        // 派单即接单：派单后状态自动变为已接单
        after.setStatus(EomsConstants.ORDER_STATUS_ACCEPTED);
        after.setAcceptTime(new Date());
        after.setUpdateBy(getCurrentUsername());
        if (StringUtils.isNotEmpty(remark))
        {
            after.setRemark(remark);
        }
        int rows = repairOrderMapper.updateRepairOrder(after);

        before.setEngineerId(engineerId);
        before.setStatus(EomsConstants.ORDER_STATUS_ACCEPTED);
        before.setAcceptTime(after.getAcceptTime());
        if (StringUtils.isNotEmpty(remark))
        {
            before.setRemark(remark);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int acceptOrder(Long orderId, String remark)
    {
        RepairOrder before = mustExist(orderId);
        String oldStatus = before.getStatus();
        if (!EomsConstants.ORDER_STATUS_WAIT_ACCEPT.equals(oldStatus))
        {
            throw new ServiceException("当前状态[" + oldStatus + "]不可接单，仅待接单状态可接单",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }
        // 接单必须已派单（有工程师）
        if (before.getEngineerId() == null)
        {
            throw new ServiceException("工单尚未派单，无法接单", EomsConstants.CODE_STATUS_ILLEGAL);
        }
        RepairOrder after = new RepairOrder();
        after.setId(orderId);
        after.setStatus(EomsConstants.ORDER_STATUS_ACCEPTED);
        after.setAcceptTime(new Date());
        after.setUpdateBy(getCurrentUsername());
        if (StringUtils.isNotEmpty(remark))
        {
            after.setRemark(remark);
        }
        int rows = repairOrderMapper.updateRepairOrder(after);

        before.setStatus(EomsConstants.ORDER_STATUS_ACCEPTED);
        before.setAcceptTime(after.getAcceptTime());
        if (StringUtils.isNotEmpty(remark))
        {
            before.setRemark(remark);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int startProcess(Long orderId, String remark)
    {
        return startProcess(orderId, remark, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int startProcess(Long orderId, String remark, String longitude, String latitude)
    {
        RepairOrder before = mustExist(orderId);
        String oldStatus = before.getStatus();
        if (!EomsConstants.ORDER_STATUS_ACCEPTED.equals(oldStatus))
        {
            throw new ServiceException("当前状态[" + oldStatus + "]不可开始维修，仅已接单状态可开始维修",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }
        RepairOrder after = new RepairOrder();
        after.setId(orderId);
        after.setStatus(EomsConstants.ORDER_STATUS_PROCESSING);
        after.setStartTime(new Date());
        after.setUpdateBy(getCurrentUsername());
        if (StringUtils.isNotEmpty(remark))
        {
            after.setRemark(remark);
        }
        // 签到位置：记录经纬度并逆地理编码为中文地址
        if (StringUtils.isNotEmpty(longitude) && StringUtils.isNotEmpty(latitude))
        {
            after.setSignInLongitude(longitude);
            after.setSignInLatitude(latitude);
            after.setSignInAddress(geoUtils.reverseGeocode(longitude, latitude));
        }
        int rows = repairOrderMapper.updateRepairOrder(after);

        before.setStatus(EomsConstants.ORDER_STATUS_PROCESSING);
        before.setStartTime(after.getStartTime());
        if (StringUtils.isNotEmpty(remark))
        {
            before.setRemark(remark);
        }
        if (StringUtils.isNotEmpty(longitude) && StringUtils.isNotEmpty(latitude))
        {
            before.setSignInLongitude(longitude);
            before.setSignInLatitude(latitude);
            before.setSignInAddress(after.getSignInAddress());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitSettlement(Long orderId, String remark)
    {
        return submitSettlement(orderId, remark, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitSettlement(Long orderId, String remark, String longitude, String latitude)
    {
        RepairOrder before = mustExist(orderId);
        String oldStatus = before.getStatus();
        if (EomsConstants.ORDER_STATUS_WAIT_SETTLEMENT.equals(oldStatus)) {
            RepairSettlement draft = new RepairSettlement();
            draft.setRepairOrderId(orderId); draft.setCreateBy(getCurrentUsername());
            draft.setSettlementNo(eomsNoGenerator.generateSettlementNo());
            return repairSettlementService.insertRepairSettlement(draft);
        }
        if (!EomsConstants.ORDER_STATUS_PROCESSING.equals(oldStatus))
        {
            throw new ServiceException("当前状态[" + oldStatus + "]不可提交结算，仅维修中状态可提交结算",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }
        // 提交结算前必须已有维修记录（H5/PC 共用；防止跳过记录直接完工）
        if (repairRecordMapper.countByRepairOrderId(orderId) <= 0)
        {
            throw new ServiceException("请先提交维修记录后再提交结算", EomsConstants.CODE_STATUS_ILLEGAL);
        }
        RepairOrder after = new RepairOrder();
        after.setId(orderId);
        after.setStatus(EomsConstants.ORDER_STATUS_WAIT_SETTLEMENT);
        after.setFinishTime(new Date());
        after.setUpdateBy(getCurrentUsername());
        if (StringUtils.isNotEmpty(remark))
        {
            after.setRemark(remark);
        }
        // 签退位置：记录经纬度并逆地理编码为中文地址
        if (StringUtils.isNotEmpty(longitude) && StringUtils.isNotEmpty(latitude))
        {
            after.setSignOutLongitude(longitude);
            after.setSignOutLatitude(latitude);
            after.setSignOutAddress(geoUtils.reverseGeocode(longitude, latitude));
        }
        int rows = repairOrderMapper.updateRepairOrder(after);

        // 自动创建结算单（草稿状态）- 维修完成进入待结算时自动触发
        RepairSettlement settlement = new RepairSettlement();
        settlement.setSettlementNo(eomsNoGenerator.generateSettlementNo());
        settlement.setRepairOrderId(orderId);
        settlement.setStatus(EomsConstants.SETTLEMENT_STATUS_DRAFT);
        settlement.setCreateBy(getCurrentUsername());
        settlement.setDelFlag("0");
        // 继承工单部门，保证部门数据权限连贯
        if (before.getDeptId() != null && before.getDeptId() > 0)
        {
            settlement.setDeptId(before.getDeptId());
        }
        if (before.getTenantId() != null)
        {
            settlement.setTenantId(before.getTenantId());
        }
        repairSettlementService.insertRepairSettlement(settlement);

        before.setStatus(EomsConstants.ORDER_STATUS_WAIT_SETTLEMENT);
        before.setFinishTime(after.getFinishTime());
        if (StringUtils.isNotEmpty(remark))
        {
            before.setRemark(remark);
        }
        if (StringUtils.isNotEmpty(longitude) && StringUtils.isNotEmpty(latitude))
        {
            before.setSignOutLongitude(longitude);
            before.setSignOutLatitude(latitude);
            before.setSignOutAddress(after.getSignOutAddress());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeOrder(Long orderId, String remark)
    {
        RepairOrder before = mustExist(orderId);
        String oldStatus = before.getStatus();
        if (!EomsConstants.ORDER_STATUS_FINISHED.equals(oldStatus))
        {
            throw new ServiceException("当前状态[" + oldStatus + "]不可关闭，仅已完成状态可关闭",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }
        RepairOrder after = new RepairOrder();
        after.setId(orderId);
        after.setStatus(EomsConstants.ORDER_STATUS_CLOSED);
        after.setUpdateBy(getCurrentUsername());
        if (StringUtils.isNotEmpty(remark))
        {
            after.setRemark(remark);
        }
        int rows = repairOrderMapper.updateRepairOrder(after);

        before.setStatus(EomsConstants.ORDER_STATUS_CLOSED);
        if (StringUtils.isNotEmpty(remark))
        {
            before.setRemark(remark);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int finishSettlement(Long orderId, String updateBy, String remark)
    {
        // P0-7 修复：结算完成工单统一走专用方法（封装状态机校验 + 操作日志），禁止 Settlement 直接用 mapper 修改工单
        RepairOrder before = mustExist(orderId);
        String oldStatus = before.getStatus();
        if (!EomsConstants.ORDER_STATUS_WAIT_SETTLEMENT.equals(oldStatus))
        {
            throw new ServiceException("当前状态[" + oldStatus + "]不可办结，仅待结算状态可办结工单",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }
        String operator = StringUtils.isNotEmpty(updateBy) ? updateBy : getCurrentUsername();
        RepairOrder after = new RepairOrder();
        after.setId(orderId);
        after.setStatus(EomsConstants.ORDER_STATUS_FINISHED);
        after.setUpdateBy(operator);
        after.setFinishTime(new Date());
        if (StringUtils.isNotEmpty(remark))
        {
            after.setRemark(remark);
        }
        // 冗余校验 WHERE 状态=WAIT_SETTLEMENT，防并发下 closeOrder 把工单改为 CLOSED 后这里又改回 FINISHED
        int rows = repairOrderMapper.updateOrderStatusWithCheck(
                orderId,
                EomsConstants.ORDER_STATUS_FINISHED,
                operator,
                after.getFinishTime(),
                after.getRemark(),
                EomsConstants.ORDER_STATUS_WAIT_SETTLEMENT);
        if (rows == 0)
        {
            throw new ServiceException("工单状态已变更，结算办结失败请刷新后重试",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }

        before.setStatus(EomsConstants.ORDER_STATUS_FINISHED);
        before.setFinishTime(after.getFinishTime());
        if (StringUtils.isNotEmpty(remark))
        {
            before.setRemark(remark);
        }
        return rows;
    }

    /**
     * 校验工单存在并返回
     */
    private RepairOrder mustExist(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("工单ID不能为空", EomsConstants.CODE_ORDER_NOT_EXISTS);
        }
        RepairOrder visible = selectRepairOrderById(id);
        if (visible == null) throw new ServiceException("维修工单不存在或无权访问", EomsConstants.CODE_ORDER_NOT_EXISTS);
        Long tenantId = currentOrderTenantId();
        if (!java.util.Objects.equals(tenantId, visible.getTenantId())) throw new ServiceException("维修工单租户不一致");
        RepairOrder locked = repairOrderMapper.lockOrder(id);
        if (locked == null || !java.util.Objects.equals(id,locked.getId())
                || !java.util.Objects.equals(tenantId,locked.getTenantId())
                || !java.util.Objects.equals(visible.getDeptId(),locked.getDeptId())
                || !java.util.Objects.equals(visible.getEngineerId(),locked.getEngineerId())
                || !java.util.Objects.equals(visible.getCreateUser(),locked.getCreateUser()))
            throw new ServiceException("工单归属已变更或无权访问，请刷新重试");
        return locked;
    }

    /**
     * 获取当前操作用户名（兼容未登录场景）
     */
    private String getCurrentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return null;
        }
    }


    /**
     * 删除 photos 字段关联的物理文件（逗号分隔；纯数字 fileId 不删本地盘）
     */
    private void deletePhotoFiles(String photos)
    {
        platformFileHelper.deleteLocalIfPath(photos);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int reverseSettlement(Long orderId, String operator, String reason)
    {
        if (repairOrderMapper.reopenAfterSettlement(orderId, operator, reason) != 1)
            throw new ServiceException("仅已办结工单可反确认结算，已关闭工单须先按业务审批处理", EomsConstants.CODE_STATUS_ILLEGAL);
        return 1;
    }

    private Long currentOrderTenantId() {
        Long id = com.ls.aegis.common.context.UserContextHolder.getContext().getTenantId();
        return id == null ? 0L : id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public int cancelAfterReversal(Long orderId, String reason)
    {
        if (reason == null || reason.isBlank() || reason.length() > 500)
            throw new ServiceException("撤销关闭原因必填且不得超过500字");
        if (orderId == null) throw new ServiceException("工单ID不能为空");
        RepairOrder visible = selectRepairOrderById(orderId);
        if (visible == null) throw new ServiceException("工单不存在或无权访问");
        Long tenantId = visible.getTenantId();
        if (tenantId == null || !java.util.Objects.equals(tenantId, currentOrderTenantId()))
            throw new ServiceException("工单租户不一致，不能撤销关闭");
        // Never acquire order before a settlement: confirm uses settlement -> order.
        List<RepairSettlement> before = repairOrderMapper.cancelSettlementHistory(tenantId, orderId);
        for (RepairSettlement history : before.stream().sorted(java.util.Comparator.comparing(RepairSettlement::getId)).toList()) {
            RepairSettlement locked = cancelSettlementMapper.lockSettlement(history.getId());
            if (locked == null || !java.util.Objects.equals(locked.getTenantId(), tenantId)
                    || !java.util.Objects.equals(locked.getRepairOrderId(), orderId))
                throw new ServiceException("结算历史已变更或租户不一致，请刷新重试");
        }
        RepairOrder locked = repairOrderMapper.lockOrder(orderId);
        if (locked == null || !java.util.Objects.equals(tenantId, locked.getTenantId())) throw new ServiceException("工单不存在或租户不一致");
        List<RepairSettlement> histories = repairOrderMapper.cancelSettlementHistory(tenantId, orderId);
        if (!before.stream().map(RepairSettlement::getId).toList().equals(histories.stream().map(RepairSettlement::getId).toList()))
            throw new ServiceException("结算历史已变更，请刷新后重试");
        if (histories.isEmpty() || histories.stream().anyMatch(s -> !"REVERSED".equals(s.getStatus()) || !java.util.Objects.equals(tenantId,s.getTenantId())))
            throw new ServiceException("必须先反确认全部结算，不能有活动或异常结算");
        boolean replay = "CLOSED".equals(locked.getStatus()) && repairOrderMapper.countCancelEvent(tenantId, orderId) == 1;
        if (!"WAIT_SETTLEMENT".equals(locked.getStatus()) && !replay)
            throw new ServiceException("仅反确认后的待结算工单可撤销关闭");
        var receivables = repairOrderMapper.lockCancelReceivables(tenantId, orderId);
        if (receivables.stream().anyMatch(r -> !java.util.Objects.equals(tenantId,r.getTenantId()) || !"VOID".equals(r.getStatus())
                || (r.getReceivedAmount()!=null && r.getReceivedAmount().signum()!=0)))
            throw new ServiceException("存在未冲销或异常应收，请先处理");
        if (repairOrderMapper.countCancelPayments(tenantId, orderId) != 0
                || repairOrderMapper.cancelIncomeNet(tenantId,orderId).signum()!=0
                || repairOrderMapper.cancelCostNet(tenantId,orderId).signum()!=0)
            throw new ServiceException("实收或成本未全部冲销，不能撤销关闭");
        cancelStockApi.assertNoActiveRepairOut(orderId, locked.getOrderNo());
        if (replay) return 0;
        String remark = (locked.getRemark()==null || locked.getRemark().isBlank() ? "" : locked.getRemark()+"\n") + "撤销关闭：" + reason.trim();
        // The immutable cancellation event always stores the full reason; never truncate legacy descriptions.
        if (remark.length()>500) remark = locked.getRemark();
        String operator = getCurrentUsername();
        if (repairOrderMapper.cancelClosed(tenantId,orderId,operator,remark)!=1) throw new ServiceException("工单状态已变更，撤销关闭失败");
        if (cancelEventMapper.insertEvent("ORDER",orderId,"CANCEL_CLOSE",histories.get(histories.size()-1).getId(),null,operator,reason.trim())!=1)
            throw new ServiceException("撤销关闭审计保存失败");
        return 1;
    }

}
