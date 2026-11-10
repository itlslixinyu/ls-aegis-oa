package com.ls.aegis.biz.eoms.service.impl.finance;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.biz.eoms.domain.Receivable;
import com.ls.aegis.biz.eoms.mapper.finance.ReceivableMapper;
import com.ls.aegis.biz.eoms.service.finance.IReceivableService;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;

/**
 * Receivable Service实现
 * 
 * @author EOMS Generator
 */
@Service
public class ReceivableServiceImpl implements IReceivableService
{
    @Autowired
    private ReceivableMapper receivableMapper;

    @Override
    public Receivable selectReceivableById(Long id)
    {
        if (EomsDataScopeHelper.shouldFilter())
        {
            Receivable query = new Receivable();
            query.setId(id);
            EomsDataScopeHelper.apply(query, false, "r");
            List<Receivable> list = receivableMapper.selectReceivableList(query);
            return list.isEmpty() ? null : list.get(0);
        }
        return receivableMapper.selectReceivableById(id);
    }

    @Override
    public List<Receivable> selectReceivableList(Receivable receivable)
    {
        EomsDataScopeHelper.apply(receivable, false, "r");
        return receivableMapper.selectReceivableList(receivable);
    }

    @Override
    public List<Receivable> selectUnsettledByCustomerId(Long customerId)
    {
        Receivable query = new Receivable();
        query.setCustomerId(customerId);
        EomsDataScopeHelper.apply(query, false, "r");
        return receivableMapper.selectUnsettledByCustomerId(query);
    }

    @Override
    public int insertReceivable(Receivable receivable)
    {
        if (receivable.getSettlementId() != null) throw new com.ls.aegis.compat.common.exception.ServiceException("业务关联仅允许由专用财务流程生成");
        if (receivable.getAmount() != null && receivable.getAmount().signum() < 0)
        {
            throw new com.ls.aegis.compat.common.exception.ServiceException("应收金额不能为负数");
        }
        // 应收金额默认 0
        if (receivable.getAmount() == null)
        {
            receivable.setAmount(java.math.BigDecimal.ZERO);
        }
        // 已收金额默认 0
        if (receivable.getReceivedAmount() == null)
        {
            receivable.setReceivedAmount(java.math.BigDecimal.ZERO);
        }
        // 欠款金额默认 = 应收 - 已收
        if (receivable.getArrearsAmount() == null)
        {
            receivable.setArrearsAmount(receivable.getAmount().subtract(receivable.getReceivedAmount()));
        }
        // 状态默认 UNPAID-未收款
        if (receivable.getStatus() == null || receivable.getStatus().isEmpty())
        {
            receivable.setStatus(com.ls.aegis.biz.eoms.constant.EomsConstants.RECEIVABLE_STATUS_UNPAID);
        }
        // 删除标志默认 0-存在
        if (receivable.getDelFlag() == null || receivable.getDelFlag().isEmpty())
        {
            receivable.setDelFlag("0");
        }
        EomsDataScopeHelper.fillOnInsert(receivable);
        return receivableMapper.insertReceivable(receivable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateReceivable(Receivable receivable)
    {
        ensureManual(receivable.getId());
        return receivableMapper.updateReceivable(receivable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteReceivableByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        for (Long id : ids) ensureManual(id);
        return receivableMapper.deleteReceivableByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteReceivableById(Long id)
    {
        return deleteReceivableByIds(new Long[]{id});
    }
    private void ensureManual(Long id)
    {
        Receivable current = receivableMapper.selectReceivableById(id);
        if (current == null || current.getRepairOrderId() != null)
            throw new com.ls.aegis.compat.common.exception.ServiceException("维修业务财务记录不可直接编辑或删除，请使用收款撤销或结算反确认");
    }

}
