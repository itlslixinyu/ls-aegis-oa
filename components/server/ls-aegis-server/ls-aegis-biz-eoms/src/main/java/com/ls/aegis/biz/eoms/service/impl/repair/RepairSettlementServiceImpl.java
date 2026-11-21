package com.ls.aegis.biz.eoms.service.impl.repair;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.SecurityUtils;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.Cost;
import com.ls.aegis.biz.eoms.domain.Receivable;
import com.ls.aegis.biz.eoms.domain.RepairMaterial;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.domain.RepairSettlement;
import com.ls.aegis.biz.eoms.mapper.finance.CostMapper;
import com.ls.aegis.biz.eoms.mapper.finance.IncomeMapper;
import com.ls.aegis.biz.eoms.mapper.finance.ReceivableMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairMaterialMapper;
import com.ls.aegis.common.api.psi.RepairStockOutApi;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairSettlementMapper;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.biz.eoms.service.repair.IRepairSettlementService;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

/**
 * RepairSettlement Service实现
 *
 * @author EOMS Generator
 */
@Service
public class RepairSettlementServiceImpl implements IRepairSettlementService
{
    @Autowired
    private RepairSettlementMapper repairSettlementMapper;

    @Autowired
    private RepairOrderMapper repairOrderMapper;

    @Autowired
    private RepairMaterialMapper repairMaterialMapper;

    @Autowired
    private com.ls.aegis.biz.eoms.mapper.finance.PaymentMapper paymentMapper;

    @Autowired
    private CostMapper costMapper;

    @Autowired
    private ReceivableMapper receivableMapper;

    @Autowired
    private IRepairOrderService repairOrderService;


    @Autowired
    private com.ls.aegis.biz.eoms.mapper.finance.FinanceEventMapper financeEventMapper;

    @Autowired
    private EomsNoGenerator eomsNoGenerator;

    @Autowired
    private RepairStockOutApi repairStockOutApi;

    @Override
    public RepairSettlement selectRepairSettlementById(Long id)
    {
        if (EomsDataScopeHelper.shouldFilter())
        {
            RepairSettlement query = new RepairSettlement();
            query.setId(id);
            EomsDataScopeHelper.apply(query, false, "s");
            List<RepairSettlement> list = repairSettlementMapper.selectRepairSettlementList(query);
            return list.isEmpty() ? null : list.get(0);
        }
        return repairSettlementMapper.selectRepairSettlementById(id);
    }

    @Override
    public List<RepairSettlement> selectRepairSettlementList(RepairSettlement repairSettlement)
    {
        EomsDataScopeHelper.apply(repairSettlement, false, "s");
        return repairSettlementMapper.selectRepairSettlementList(repairSettlement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRepairSettlement(RepairSettlement repairSettlement)
    {
        RepairOrder order = repairOrderService.selectRepairOrderById(repairSettlement.getRepairOrderId());
        order = order == null ? null : repairOrderMapper.lockOrder(order.getId());
        if (order == null || !"WAIT_SETTLEMENT".equals(order.getStatus())) throw new ServiceException("仅待结算工单可创建结算草稿");
        repairSettlement.setStatus("DRAFT");
        repairSettlement.setDelFlag("0");
        repairSettlement.setDeptId(order.getDeptId());
        repairSettlement.setTenantId(order.getTenantId());
        // 检查是否已存在结算单（同一工单只能有一个结算单，幂等处理）
        if (repairSettlement.getRepairOrderId() != null)
        {
            RepairSettlement existing = repairSettlementMapper.selectSettlementByOrderId(repairSettlement.getRepairOrderId());
            if (existing != null)
            {
                repairSettlement.setId(existing.getId());
                return 1;
            }
        }
        if (StringUtils.isEmpty(repairSettlement.getStatus()))
        {
            repairSettlement.setStatus(EomsConstants.SETTLEMENT_STATUS_DRAFT);
        }
        if (StringUtils.isEmpty(repairSettlement.getDelFlag()))
        {
            repairSettlement.setDelFlag("0");
        }
        EomsDataScopeHelper.fillOnInsert(repairSettlement);
        return repairSettlementMapper.insertRepairSettlement(repairSettlement);
    }

    @Override
    public RepairSettlement selectSettlementByOrderId(Long repairOrderId)
    {
        if (EomsDataScopeHelper.shouldFilter())
        {
            RepairSettlement query = new RepairSettlement();
            query.setRepairOrderId(repairOrderId);
            EomsDataScopeHelper.apply(query, false, "s");
            List<RepairSettlement> list = repairSettlementMapper.selectRepairSettlementList(query);
            return list.isEmpty() ? null : list.get(0);
        }
        return repairSettlementMapper.selectSettlementByOrderId(repairOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRepairSettlement(RepairSettlement repairSettlement)
    {
        RepairSettlement visible = selectRepairSettlementById(repairSettlement.getId());
        RepairSettlement current = visible == null ? null : repairSettlementMapper.lockSettlement(visible.getId());
        if (current == null || !"DRAFT".equals(current.getStatus())) throw new ServiceException("仅结算草稿可编辑");
        repairSettlement.setRepairOrderId(current.getRepairOrderId());
        repairSettlement.setStatus("DRAFT");
        repairSettlement.setDelFlag("0");
        // 若结算单号为空则自动生成（兼容历史数据）
        if (StringUtils.isEmpty(repairSettlement.getSettlementNo()) && repairSettlement.getId() != null)
        {
            RepairSettlement existing = repairSettlementMapper.selectRepairSettlementById(repairSettlement.getId());
            if (existing != null && StringUtils.isEmpty(existing.getSettlementNo()))
            {
                repairSettlement.setSettlementNo(eomsNoGenerator.generateSettlementNo());
            }
        }
        return repairSettlementMapper.updateRepairSettlement(repairSettlement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRepairSettlementByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        for (Long id : java.util.Arrays.stream(ids).distinct().sorted().toList()) {
            RepairSettlement visible = selectRepairSettlementById(id);
            RepairSettlement current = visible == null ? null : repairSettlementMapper.lockSettlement(id);
            if (current == null || !"DRAFT".equals(current.getStatus())) throw new ServiceException("仅草稿可删除，已确认结算请使用反确认接口");
        }
        return repairSettlementMapper.deleteRepairSettlementByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRepairSettlementById(Long id)
    {
        return deleteRepairSettlementByIds(new Long[]{id});
    }

    /**
     * 结算确认（8动作原子事务）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmSettlement(Long settlementId, String remark)
    {
        // ===== 动作1：校验结算单状态 =====
        if (settlementId == null)
        {
            throw new ServiceException("结算单ID不能为空", EomsConstants.CODE_STATUS_ILLEGAL);
        }
        RepairSettlement visible = selectRepairSettlementById(settlementId);
        RepairSettlement settlement = visible == null ? null : repairSettlementMapper.lockSettlement(settlementId);
        if (settlement == null)
        {
            throw new ServiceException("结算单不存在", EomsConstants.CODE_STATUS_ILLEGAL);
        }
        if (!EomsConstants.SETTLEMENT_STATUS_DRAFT.equals(settlement.getStatus()))
        {
            throw new ServiceException("当前状态[" + settlement.getStatus() + "]不可确认，仅草稿状态可确认",
                    EomsConstants.CODE_SETTLEMENT_CONFIRMED);
        }

        Long orderId = settlement.getRepairOrderId();
        RepairOrder order = repairOrderMapper.lockOrder(orderId);
        if (order == null)
        {
            throw new ServiceException("关联维修工单不存在", EomsConstants.CODE_ORDER_NOT_EXISTS);
        }
        // 工单状态必须为待结算
        if (!EomsConstants.ORDER_STATUS_WAIT_SETTLEMENT.equals(order.getStatus()))
        {
            throw new ServiceException("工单状态[" + order.getStatus() + "]非待结算，无法确认结算",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }

        // ===== 动作2：自动计算金额 =====
        List<RepairMaterial> materialList = repairMaterialMapper.lockByOrder(orderId);

        BigDecimal materialAmount = BigDecimal.ZERO;
        for (RepairMaterial rm : materialList)
        {
            BigDecimal saleAmt = nullToZero(rm.getSaleAmount());
            materialAmount = materialAmount.add(saleAmt);
        }

        BigDecimal serviceFee = nullToZero(settlement.getServiceFee());
        BigDecimal laborAmount = nullToZero(settlement.getLaborAmount());
        BigDecimal otherAmount = nullToZero(settlement.getOtherAmount());
        BigDecimal laborCost = nullToZero(settlement.getLaborCost());
        BigDecimal otherCost = nullToZero(settlement.getOtherCost());
        for (BigDecimal value : List.of(serviceFee, laborAmount, otherAmount, laborCost, otherCost, materialAmount)) {
            if (value.signum() < 0 || value.stripTrailingZeros().scale() > 2 || value.precision() - value.scale() > 10)
                throw new ServiceException("结算金额必须为非负数且最多10位整数、2位小数");
        }

        BigDecimal totalIncome = serviceFee.add(materialAmount).add(laborAmount).add(otherAmount);

        String operator = getCurrentUsername();
        Date now = new Date();

        // 生成结算单号（若尚未生成，兼容历史数据）
        String settlementNo = settlement.getSettlementNo();
        if (StringUtils.isEmpty(settlementNo))
        {
            settlementNo = eomsNoGenerator.generateSettlementNo();
        }

        // ===== 动作3：维修出库改记 PSI stock-out（类型 REPAIR），禁止手改库存数量 =====
        RepairStockOutApi.Cmd outCmd = new RepairStockOutApi.Cmd();
        outCmd.setOrderId(orderId);
        outCmd.setSettlementId(settlementId);
        outCmd.setOrderNo(order.getOrderNo());
        outCmd.setCustomerId(order.getCustomerId());
        java.util.ArrayList<RepairStockOutApi.Item> outItems = new java.util.ArrayList<>();
        for (RepairMaterial rm : materialList)
        {
            if (rm.getMaterialId() == null || rm.getQuantity() == null)
            {
                throw new ServiceException("结算用料必须指定商品和数量");
            }
            BigDecimal qty = rm.getQuantity();
            if (qty.compareTo(BigDecimal.ZERO) <= 0)
            {
                throw new ServiceException("结算用料数量必须为正");
            }
            RepairStockOutApi.Item line = new RepairStockOutApi.Item();
            line.setGoodsId(rm.getMaterialId());
            line.setGoodsName(rm.getMaterialName());
            line.setQuantity(qty);
            line.setTaxInclusivePrice(rm.getSalePrice());
            line.setWarehouseId(rm.getWarehouseId());
            outItems.add(line);
        }
        outCmd.setItems(outItems);
        BigDecimal materialCost;
        try
        {
            materialCost = repairStockOutApi.confirmRepairOut(outCmd);
        }
        catch (RuntimeException ex)
        {
            throw new ServiceException(StringUtils.isNotEmpty(ex.getMessage()) ? ex.getMessage() : "维修出库失败",
                    EomsConstants.CODE_STOCK_NOT_ENOUGH);
        }

        // 材料估算成本仅用于报价，入账必须使用同事务PSI实际FIFO结果。
        if (materialCost == null) throw new ServiceException("维修出库未返回实际FIFO成本");
        BigDecimal totalCost = materialCost.add(laborCost).add(otherCost);
        BigDecimal profitAmount = totalIncome.subtract(totalCost);
        for (BigDecimal value : List.of(materialCost, totalIncome, totalCost)) {
            if (value.signum() < 0 || value.stripTrailingZeros().scale() > 2 || value.precision() - value.scale() > 10)
                throw new ServiceException("实际结算金额必须为非负数且最多10位整数、2位小数");
        }
        if (profitAmount.stripTrailingZeros().scale() > 2 || profitAmount.precision() - profitAmount.scale() > 10)
            throw new ServiceException("实际结算利润超出金额范围");

        // ===== 动作4：CAS原子更新结算单状态为CONFIRMED（防并发双重确认） =====
        String remarkVal = StringUtils.isNotEmpty(remark) ? remark : null;
        int casRows = repairSettlementMapper.casConfirmSettlement(
                settlementId,
                EomsConstants.SETTLEMENT_STATUS_DRAFT,
                EomsConstants.SETTLEMENT_STATUS_CONFIRMED,
                settlementNo,
                serviceFee,
                laborAmount,
                otherAmount,
                materialAmount,
                materialCost,
                totalIncome,
                totalCost,
                profitAmount,
                operator,
                remarkVal);
        if (casRows != 1)
        {
            throw new ServiceException("结算单已被其他事务处理，请刷新后重试",
                    EomsConstants.CODE_SETTLEMENT_CONFIRMED);
        }

        // 同步到日志记录用的快照对象
        settlement.setSettlementNo(settlementNo);
        settlement.setServiceFee(serviceFee);
        settlement.setLaborAmount(laborAmount);
        settlement.setOtherAmount(otherAmount);
        settlement.setMaterialAmount(materialAmount);
        settlement.setMaterialCost(materialCost);
        settlement.setTotalIncome(totalIncome);
        settlement.setTotalCost(totalCost);
        settlement.setProfitAmount(profitAmount);
        settlement.setStatus(EomsConstants.SETTLEMENT_STATUS_CONFIRMED);

        // ===== 动作5：通过 IRepairOrderService.finishSettlement 办结工单（封装状态机校验+操作日志+CAS状态校验）
        //          禁止直接使用 repairOrderMapper.updateRepairOrder 绕过校验与日志
        repairOrderService.finishSettlement(orderId, operator, remarkVal);

        // ===== 动作6：生成分项成本记录（MATERIAL/LABOR/OTHER） =====
        // P0-1 修复：结算确认只产生应收账款（动作8），不产生实收收入。
        //   实收收入（Income）应在客户实际付款（PaymentServiceImpl.verifyReceivable）时才记录，
        //   否则财务报表会把"应收"误记为"实收"，导致收入虚高、利润虚增。
        if (materialCost.compareTo(BigDecimal.ZERO) > 0)
        {
            insertCost(settlementId, orderId, EomsConstants.COST_TYPE_MATERIAL, materialCost,
                    "材料成本", operator, now);
        }
        if (laborCost.compareTo(BigDecimal.ZERO) > 0)
        {
            insertCost(settlementId, orderId, EomsConstants.COST_TYPE_LABOR, laborCost,
                    "人工成本", operator, now);
        }
        if (otherCost.compareTo(BigDecimal.ZERO) > 0)
        {
            insertCost(settlementId, orderId, EomsConstants.COST_TYPE_OTHER, otherCost,
                    "其它成本", operator, now);
        }

        // ===== 动作8：生成应收账款记录（同一工单仅允许存在一条未删除应收，防重复生成导致欠款虚增） =====
        Receivable existReceivableQuery = new Receivable();
        existReceivableQuery.setRepairOrderId(orderId);
        List<Receivable> existReceivables = receivableMapper.lockByOrder(orderId);
        if (existReceivables != null && !existReceivables.isEmpty())
        {
            throw new ServiceException("工单已有活动应收，不能再次确认结算，请先对账处理");
        }
        else
        {
            Receivable receivable = new Receivable();
            receivable.setReceivableNo(eomsNoGenerator.generateReceivableNo());
            receivable.setCustomerId(order.getCustomerId());
            receivable.setRepairOrderId(orderId);
            receivable.setSettlementId(settlementId);
            receivable.setAmount(totalIncome);
            receivable.setReceivedAmount(BigDecimal.ZERO);
            receivable.setArrearsAmount(totalIncome);
            receivable.setStatus(totalIncome.signum()==0 ? EomsConstants.RECEIVABLE_STATUS_PAID : EomsConstants.RECEIVABLE_STATUS_UNPAID);
            receivable.setCreateBy(operator);
            receivable.setDelFlag("0");
            receivable.setRemark("工单[" + order.getOrderNo() + "]结算应收");
            // 优先继承工单部门，便于部门数据权限连贯
            if (order.getDeptId() != null && order.getDeptId() > 0)
            {
                receivable.setDeptId(order.getDeptId());
            }
            if (order.getTenantId() != null)
            {
                receivable.setTenantId(order.getTenantId());
            }
            EomsDataScopeHelper.fillOnInsert(receivable);
            receivableMapper.insertReceivable(receivable);
        }


        return 1;
    }

    /**
     * 插入成本记录
     */
    private void insertCost(Long settlementId, Long orderId, String costType, BigDecimal amount,
                            String desc, String operator, Date now)
    {
        Cost cost = new Cost();
        cost.setSettlementId(settlementId);
        cost.setRepairOrderId(orderId);
        cost.setCostType(costType);
        cost.setAmount(amount);
        cost.setDescription(desc);
        cost.setCreateBy(operator);
        cost.setDelFlag("0");
        costMapper.insertCost(cost);
    }

    /**
     * null 转 BigDecimal.ZERO
     */
    private BigDecimal nullToZero(BigDecimal val)
    {
        return val == null ? BigDecimal.ZERO : val;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int reverseSettlement(Long settlementId, String reason)
    {
        if (StringUtils.isEmpty(reason) || reason.length() > 500) throw new ServiceException("请输入500字以内反确认原因");
        RepairSettlement visible = selectRepairSettlementById(settlementId);
        RepairSettlement settlement = visible == null ? null : repairSettlementMapper.lockSettlement(settlementId);
        if (settlement == null) throw new ServiceException("结算单不存在或无权访问");
        if ("REVERSED".equals(settlement.getStatus())) return 0;
        if (!"CONFIRMED".equals(settlement.getStatus())) throw new ServiceException("仅已确认结算可反确认");
        Long orderId = settlement.getRepairOrderId();
        List<Receivable> receivables = receivableMapper.lockByOrder(orderId);
        if (receivables.size() != 1) throw new ServiceException("应收关联不唯一，请先对账处理");
        Receivable receivable = receivables.get(0);
        if (!settlementId.equals(receivable.getSettlementId())) throw new ServiceException("历史应收缺少明确结算关联，请先对账");
        if (nullToZero(receivable.getReceivedAmount()).signum() != 0 || paymentMapper.countActiveByReceivable(receivable.getId()) != 0)
            throw new ServiceException("结算已有收款，请先逐笔撤销收款后反确认");
        RepairOrder order = repairOrderMapper.selectRepairOrderById(orderId);
        if (order == null || !"FINISHED".equals(order.getStatus())) throw new ServiceException("仅已办结且未关闭工单可反确认");
        List<Cost> costs = costMapper.lockByOrder(orderId);
        BigDecimal costTotal = BigDecimal.ZERO;
        for (Cost cost : costs) {
            if (cost.getSettlementId() == null) throw new ServiceException("历史成本缺少明确结算关联，请先对账");
            if (settlementId.equals(cost.getSettlementId()) && cost.getReversalOfId() == null) costTotal = costTotal.add(nullToZero(cost.getAmount()));
        }
        if (costTotal.compareTo(nullToZero(settlement.getTotalCost())) != 0) throw new ServiceException("成本明细与结算金额不一致，请先对账");
        String operator = getCurrentUsername();
        // PSI owns inventory and FIFO restoration. Its failure rolls back this entire transaction.
        repairStockOutApi.reverseRepairOut(orderId, order.getOrderNo());
        for (Cost original : costs) {
            if (!settlementId.equals(original.getSettlementId()) || original.getReversalOfId() != null) continue;
            Cost reversal = new Cost();
            reversal.setRepairOrderId(orderId); reversal.setSettlementId(settlementId);
            reversal.setReversalOfId(original.getId()); reversal.setCostType(original.getCostType());
            reversal.setAmount(original.getAmount().negate()); reversal.setDescription("反确认结算#"+settlementId+"，冲销成本#"+original.getId());
            reversal.setDelFlag("0"); reversal.setCreateBy(operator); costMapper.insertCost(reversal);
        }
        if (receivableMapper.voidReceivable(receivable.getId(), operator) != 1) throw new ServiceException("应收已变更，反确认失败");
        repairOrderService.reverseSettlement(orderId, operator, reason);
        if (repairSettlementMapper.reverseSettlement(settlementId, operator, reason) != 1) throw new ServiceException("结算状态已变更");
        financeEventMapper.insertEvent("SETTLEMENT", settlementId, "REVERSE", orderId,
                nullToZero(settlement.getTotalIncome()).negate(), operator, reason);
        return 1;
    }

}
