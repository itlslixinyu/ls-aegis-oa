package com.ls.aegis.biz.psi.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.model.entity.UnitDO;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.common.api.psi.H5CustomerAccessApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.continew.starter.core.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class H5CustomerAccessApiImpl implements H5CustomerAccessApi {
    private final UnitMapper unitMapper;
    @Override
    public Customer requireManagedCustomer(Long customerId) {
        if (customerId == null) throw new BusinessException("客户不存在或无权操作");
        // selectList retains DataPermission; selectById/selectOne do not provide this contract.
        var rows = unitMapper.selectList(Wrappers.<UnitDO>lambdaQuery()
            .eq(UnitDO::getId, customerId).eq(UnitDO::getTenantId, TenantFillHelper.currentTenantId()));
        if (rows.size() != 1) throw new BusinessException("客户不存在或无权操作");
        return available(rows.get(0));
    }
    @Override
    public Customer requireAvailableCustomer(Long tenantId, Long customerId) {
        if (tenantId == null || customerId == null) throw new BusinessException("客户授权无效");
        return available(unitMapper.selectH5Customer(tenantId, customerId));
    }
    private Customer available(UnitDO unit) {
        if (unit == null || unit.getTenantId() == null || unit.getStatus() != DisEnableStatusEnum.ENABLE
            || (unit.getUnitType() != UnitTypeEnum.CUSTOMER && unit.getUnitType() != UnitTypeEnum.BOTH)) {
            throw new BusinessException("客户不存在或已停用");
        }
        return new Customer(unit.getId(), unit.getTenantId(), unit.getUnitCode(), unit.getUnitName());
    }
}
