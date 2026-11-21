package com.ls.aegis.biz.eoms.service.impl.finance;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.biz.eoms.domain.Cost;
import com.ls.aegis.biz.eoms.mapper.finance.CostMapper;
import com.ls.aegis.biz.eoms.service.finance.ICostService;

/**
 * Cost Service实现
 * 
 * @author EOMS Generator
 */
@Service
public class CostServiceImpl implements ICostService
{
    @Autowired
    private CostMapper costMapper;

    @Autowired
    private com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper repairOrderMapper;

    @Autowired
    private com.ls.aegis.biz.eoms.service.repair.IRepairOrderService repairOrderService;

    @Override
    public Cost selectCostById(Long id)
    {
        return costMapper.selectCostById(id);
    }

    @Override
    public List<Cost> selectCostList(Cost cost)
    {
        return costMapper.selectCostList(cost);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCost(Cost cost)
    {
        if (cost.getSettlementId() != null || cost.getReversalOfId() != null) throw new com.ls.aegis.compat.common.exception.ServiceException("业务关联仅允许由专用财务流程生成");
        if (cost.getRepairOrderId() != null) {
            if (repairOrderService.selectRepairOrderById(cost.getRepairOrderId()) == null)
                throw new com.ls.aegis.compat.common.exception.ServiceException("工单不存在或无权访问");
            var order = repairOrderMapper.lockOrder(cost.getRepairOrderId());
            if (order == null || java.util.Set.of("FINISHED", "CLOSED").contains(order.getStatus()))
                throw new com.ls.aegis.compat.common.exception.ServiceException("已结算或关闭工单不能直接追加成本");
        }
        // 删除标志默认 0-存在
        if (cost.getDelFlag() == null || cost.getDelFlag().isEmpty())
        {
            cost.setDelFlag("0");
        }
        return costMapper.insertCost(cost);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCost(Cost cost)
    {
        if (cost.getRepairOrderId()!=null || cost.getSettlementId()!=null || cost.getReversalOfId()!=null)
            throw new com.ls.aegis.compat.common.exception.ServiceException("手工成本不可变更工单或业务关联");
        ensureManual(cost.getId());
        return costMapper.updateCost(cost);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCostByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        for (Long id : ids) ensureManual(id);
        return costMapper.deleteCostByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCostById(Long id)
    {
        return deleteCostByIds(new Long[]{id});
    }
    private void ensureManual(Long id)
    {
        Cost current = costMapper.selectCostById(id);
        if (current == null || current.getRepairOrderId() != null)
            throw new com.ls.aegis.compat.common.exception.ServiceException("维修业务财务记录不可直接编辑或删除，请使用收款撤销或结算反确认");
    }

}
