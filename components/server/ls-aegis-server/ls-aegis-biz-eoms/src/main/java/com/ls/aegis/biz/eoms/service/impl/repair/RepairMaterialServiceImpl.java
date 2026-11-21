package com.ls.aegis.biz.eoms.service.impl.repair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.biz.eoms.domain.RepairMaterial;
import com.ls.aegis.biz.eoms.mapper.repair.RepairMaterialMapper;
import com.ls.aegis.biz.eoms.service.repair.IRepairMaterialService;

/**
 * 工单用料：material_id 指向 psi_goods.id，不改库存；结算确认时走 PSI stock-out
 */
@Service
public class RepairMaterialServiceImpl implements IRepairMaterialService
{
    @Autowired
    private RepairMaterialMapper repairMaterialMapper;

    @Autowired
    private com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper repairOrderMapper;

    @Autowired
    private com.ls.aegis.biz.eoms.service.repair.IRepairOrderService repairOrderService;

    @Override
    public RepairMaterial selectRepairMaterialById(Long id)
    {
        if (id == null) return null;
        RepairMaterial query = new RepairMaterial(); query.setId(id);
        List<RepairMaterial> visible = selectRepairMaterialList(query);
        return visible.isEmpty() ? null : visible.get(0);
    }

    @Override
    public List<RepairMaterial> selectRepairMaterialList(RepairMaterial repairMaterial)
    {
        Long tenantId = authenticatedTenant();
        if (repairMaterial == null) repairMaterial = new RepairMaterial();
        var params = repairMaterial.getParams();
        Object begin = params.get("beginTime"), end = params.get("endTime"); params.clear();
        if (begin != null) params.put("beginTime", begin);
        if (end != null) params.put("endTime", end);
        params.put("materialTenantId", tenantId);
        com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper.apply(repairMaterial, true, "o");
        return repairMaterialMapper.selectRepairMaterialList(repairMaterial);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRepairMaterial(RepairMaterial repairMaterial)
    {
        if (repairMaterial == null) throw new ServiceException("用料不能为空");
        lockEditableOrder(repairMaterial.getRepairOrderId());
        validateLine(repairMaterial);
        fillAmounts(repairMaterial);
        repairMaterial.setId(null);
        repairMaterial.setDelFlag("0");
        repairMaterial.setCreateBy(com.ls.aegis.common.context.UserContextHolder.getContext().getUsername());
        repairMaterial.setCreateTime(null); repairMaterial.setUpdateBy(null); repairMaterial.setUpdateTime(null);
        int changed = repairMaterialMapper.insertRepairMaterial(repairMaterial);
        if (changed != 1) throw new ServiceException("用料写入失败");
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRepairMaterial(RepairMaterial repairMaterial)
    {
        if (repairMaterial == null || repairMaterial.getId() == null) throw new ServiceException("用料ID不能为空");
        RepairMaterial original = selectRepairMaterialById(repairMaterial.getId());
        if (original == null) throw new ServiceException("用料不存在或无权访问");
        lockEditableOrder(original.getRepairOrderId());
        requireCurrent(original);
        if (repairMaterial.getRepairOrderId() != null && !original.getRepairOrderId().equals(repairMaterial.getRepairOrderId()))
            throw new ServiceException("用料不可迁移工单");
        repairMaterial.setRepairOrderId(original.getRepairOrderId());
        validateLine(repairMaterial);
        fillAmounts(repairMaterial);
        repairMaterial.setDelFlag("0");
        repairMaterial.setUpdateBy(com.ls.aegis.common.context.UserContextHolder.getContext().getUsername());
        int changed = repairMaterialMapper.updateRepairMaterial(repairMaterial);
        if (changed != 1) throw new ServiceException("用料更新失败");
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRepairMaterialByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        if (java.util.Arrays.stream(ids).anyMatch(java.util.Objects::isNull)) throw new ServiceException("用料ID不能为空");
        ids = java.util.Arrays.stream(ids).distinct().sorted().toArray(Long[]::new);
        java.util.List<RepairMaterial> lines = new java.util.ArrayList<>();
        for (Long id : ids) {
            RepairMaterial line = selectRepairMaterialById(id);
            if (line == null) throw new ServiceException("用料不存在");
            lines.add(line);
        }
        for (Long orderId : lines.stream().map(RepairMaterial::getRepairOrderId).distinct().sorted().toList()) lockEditableOrder(orderId);
        for (RepairMaterial line : lines) requireCurrent(line);
        int changed = repairMaterialMapper.deleteRepairMaterialByIds(ids);
        if (changed != ids.length) throw new ServiceException("用料删除失败");
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRepairMaterialById(Long id)
    {
        return deleteRepairMaterialByIds(new Long[]{id});
    }

    private Long authenticatedTenant()
    {
        cn.dev33.satoken.stp.StpUtil.checkLogin();
        var user = com.ls.aegis.common.context.UserContextHolder.getContext();
        if (user == null || user.getId() == null) throw new ServiceException("用料身份无效");
        Long tenantId = top.continew.starter.extension.tenant.context.TenantContextHolder.isTenantEnabled()
            ? user.getTenantId() : cn.hutool.extra.spring.SpringUtil.getBean(com.ls.aegis.common.config.TenantExtensionProperties.class).getDefaultTenantId();
        if (tenantId == null) throw new ServiceException("用料租户身份无效");
        return tenantId;
    }

    private void requireCurrent(RepairMaterial visible)
    {
        if (repairMaterialMapper.lockMaterial(visible.getId(), visible.getRepairOrderId()) == null)
            throw new ServiceException("用料已变更，请刷新重试");
    }

    private void lockEditableOrder(Long orderId)
    {
        Long tenantId = authenticatedTenant();
        var visible = orderId == null ? null : repairOrderService.selectRepairOrderById(orderId);
        if (visible == null) throw new ServiceException("工单不存在或无权访问");
        var current = repairOrderMapper.lockOrder(orderId);
        if (current == null || !tenantId.equals(current.getTenantId())
            || !java.util.Objects.equals(visible.getTenantId(), current.getTenantId())
            || !java.util.Objects.equals(visible.getDeptId(), current.getDeptId())
            || !java.util.Objects.equals(visible.getEngineerId(), current.getEngineerId())
            || !java.util.Objects.equals(visible.getCreateUser(), current.getCreateUser()))
            throw new ServiceException("工单已变更，请刷新重试");
        if (!java.util.Set.of("WAIT_ACCEPT", "ACCEPTED", "PROCESSING", "WAIT_SETTLEMENT").contains(current.getStatus()))
            throw new ServiceException("已结算或关闭工单不可修改用料，请先反确认结算");
    }

    private void validateLine(RepairMaterial line)
    {
        if (line.getRepairOrderId() == null || line.getRepairOrderId() <= 0) throw new ServiceException("工单不能为空");
        if (line.getMaterialId() == null || line.getMaterialId() <= 0) throw new ServiceException("请选择进销存商品");
        if (line.getWarehouseId() == null || line.getWarehouseId() <= 0) throw new ServiceException("请选择出库仓库");
        if (line.getMaterialName() == null || line.getMaterialName().trim().isEmpty() || line.getMaterialName().length() > 100)
            throw new ServiceException("商品名称必须为1至100字");
        line.setMaterialName(line.getMaterialName().trim());
        if (line.getRemark() != null && line.getRemark().length() > 500) throw new ServiceException("备注不能超过500字");
        decimal(line.getQuantity(), 8, true, "使用数量");
        if (line.getSalePrice() == null) line.setSalePrice(BigDecimal.ZERO);
        if (line.getCostPrice() == null) line.setCostPrice(BigDecimal.ZERO);
        decimal(line.getSalePrice(), 10, false, "销售单价");
        decimal(line.getCostPrice(), 10, false, "成本单价");
        if (repairMaterialMapper.countUsableReferences(authenticatedTenant(), line.getMaterialId(), line.getWarehouseId()) != 1)
            throw new ServiceException("商品或仓库不存在、已停用或不属于当前工单租户");
    }

    private void decimal(BigDecimal value, int integers, boolean positive, String name)
    {
        if (value == null || value.signum() < 0 || (positive && value.signum() == 0))
            throw new ServiceException(name + (positive ? "必须大于0" : "不能为负数"));
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() > 2 || normalized.precision() - normalized.scale() > integers)
            throw new ServiceException(name + "超出数据库金额或数量精度");
    }

    private void fillAmounts(RepairMaterial line)
    {
        line.setSaleAmount(line.getQuantity().multiply(line.getSalePrice()).setScale(2, RoundingMode.HALF_UP));
        line.setCostAmount(line.getQuantity().multiply(line.getCostPrice()).setScale(2, RoundingMode.HALF_UP));
        decimal(line.getSaleAmount(), 10, false, "销售金额");
        decimal(line.getCostAmount(), 10, false, "成本金额");
    }
}
