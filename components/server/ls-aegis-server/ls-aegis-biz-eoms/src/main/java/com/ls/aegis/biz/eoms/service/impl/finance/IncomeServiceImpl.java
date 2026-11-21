package com.ls.aegis.biz.eoms.service.impl.finance;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.eoms.domain.Income;
import com.ls.aegis.biz.eoms.mapper.finance.IncomeMapper;
import com.ls.aegis.biz.eoms.service.finance.IIncomeService;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;

/**
 * Income Service实现
 *
 * @author EOMS Generator
 */
@Service
public class IncomeServiceImpl implements IIncomeService
{
    @Autowired
    private IncomeMapper incomeMapper;

    @Autowired
    private com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper repairOrderMapper;
    @Autowired
    private com.ls.aegis.biz.eoms.service.repair.IRepairOrderService repairOrderService;

    @Autowired
    private EomsNoGenerator eomsNoGenerator;

    @Override
    public Income selectIncomeById(Long id)
    {
        if (id == null) return null;
        Income query = new Income();query.setId(id);
        List<Income> visible = selectIncomeList(query);
        return visible.isEmpty() ? null : visible.get(0);
    }

    @Override
    public List<Income> selectIncomeList(Income income)
    {
        cn.dev33.satoken.stp.StpUtil.checkLogin();
        var user = com.ls.aegis.common.context.UserContextHolder.getContext();
        if (user == null || user.getId() == null)
            throw new com.ls.aegis.compat.common.exception.ServiceException("收入查询身份无效");
        // Normal platform logins intentionally have tenantId=null when tenant support is disabled.
        // Only the authoritative feature switch permits the configured default; enabled tenants never fall back.
        Long tenantId = top.continew.starter.extension.tenant.context.TenantContextHolder.isTenantEnabled()
            ? user.getTenantId() : cn.hutool.extra.spring.SpringUtil.getBean(com.ls.aegis.common.config.TenantExtensionProperties.class).getDefaultTenantId();
        if (tenantId == null) throw new com.ls.aegis.compat.common.exception.ServiceException("收入查询租户身份无效");
        if (income == null) income = new Income();
        // Only date filters are client-controlled. Scope and tenant are always rebuilt by the server,
        // including ALL/admin roles for which the shared scope helper intentionally adds no filter.
        var params = income.getParams();Object begin = params.get("beginTime"), end = params.get("endTime");params.clear();
        if (begin != null) params.put("beginTime", begin);
        if (end != null) params.put("endTime", end);
        params.put("incomeTenantId", tenantId);
        com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper.apply(income, true, "o");
        return incomeMapper.selectIncomeList(income);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertIncome(Income income)
    {
        if (income.getPaymentId() != null || income.getReversalOfId() != null) throw new com.ls.aegis.compat.common.exception.ServiceException("业务关联仅允许由专用财务流程生成");
        if (income.getRepairOrderId() != null) {
            if (repairOrderService.selectRepairOrderById(income.getRepairOrderId()) == null)
                throw new com.ls.aegis.compat.common.exception.ServiceException("工单不存在或无权访问");
            var order = repairOrderMapper.lockOrder(income.getRepairOrderId());
            if (order == null || java.util.Set.of("FINISHED", "CLOSED").contains(order.getStatus()))
                throw new com.ls.aegis.compat.common.exception.ServiceException("已结算或关闭工单不能直接追加收入");
        }
        // 收入单编号 NOT NULL UNIQUE，空时由 EomsNoGenerator 基于 Redis INCR 原子生成
        if (StringUtils.isEmpty(income.getIncomeNo()))
        {
            income.setIncomeNo(eomsNoGenerator.generateIncomeNo());
        }
        // 删除标志默认 0-存在
        if (StringUtils.isEmpty(income.getDelFlag()))
        {
            income.setDelFlag("0");
        }
        return incomeMapper.insertIncome(income);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateIncome(Income income)
    {
        if (income.getRepairOrderId()!=null || income.getPaymentId()!=null || income.getReversalOfId()!=null)
            throw new com.ls.aegis.compat.common.exception.ServiceException("手工收入不可变更工单或业务关联");
        ensureManual(income.getId());
        return incomeMapper.updateIncome(income);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteIncomeByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        for (Long id : ids) ensureManual(id);
        return incomeMapper.deleteIncomeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteIncomeById(Long id)
    {
        return deleteIncomeByIds(new Long[]{id});
    }
    private void ensureManual(Long id)
    {
        Income current = incomeMapper.selectIncomeById(id);
        if (current == null || current.getRepairOrderId() != null)
            throw new com.ls.aegis.compat.common.exception.ServiceException("维修业务财务记录不可直接编辑或删除，请使用收款撤销或结算反确认");
    }

}
