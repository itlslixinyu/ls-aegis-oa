package com.ls.aegis.biz.eoms.service.h5;

import com.ls.aegis.biz.eoms.domain.RepairApply;
import com.ls.aegis.biz.eoms.mapper.customer.CustomerMapper;
import com.ls.aegis.biz.eoms.service.repair.IRepairApplyService;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.biz.psi.service.UnitService;
import com.ls.aegis.biz.psi.model.req.UnitReq;
import com.ls.aegis.biz.psi.enums.UnitNatureEnum;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EngineerFieldRepairService {
    private final EngineerPoolService pool;
    private final CustomerMapper customers;
    private final IRepairApplyService applies;
    private final IdempotentHelper idempotency;
    private final H5AttachmentService attachments;

    public record Request(@NotNull UUID requestId, @NotNull @Positive Long customerId,
                          @NotBlank @Size(max=50) String contactName, @NotBlank @Size(max=30) String contactPhone,
                          @NotBlank @Size(max=500) String address, @NotBlank @Size(min=5,max=500) String fault,
                          @Size(max=32) String equipment, @Size(max=9) List<@NotNull @Positive Long> photos) {}

    private RepairApply query(Long customerId) {
        var query = new RepairApply();
        query.setTenantId(pool.authorizeEngineer());
        query.setCustomerId(customerId);
        EomsDataScopeHelper.apply(query, false, "a");
        return query;
    }

    public List<Map<String,String>> customers(String term) {
        var query = query(null);
        query.setCustomerName(term.trim());
        return customers.fieldCustomers(query);
    }

    private final UnitService units;

    public record NewCustomer(@NotBlank @Size(max=128) String name, @NotNull @Pattern(regexp="ENTERPRISE|PERSON") String nature) {}

    @Transactional(rollbackFor=Exception.class)
    public Map<String,String> createCustomer(NewCustomer input) {
        var scope = query(null);
        String name = input.name().strip();
        scope.setCustomerName(name);
        String key = UUID.nameUUIDFromBytes(name.toLowerCase(java.util.Locale.ROOT)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        String id = idempotency.execute(scope.getTenantId(), "field-customer", key, () -> {
            if (customers.fieldUnitNameExists(scope)) throw new ServiceException("无法新建同名档案，请核对客户全称或联系管理员");
            var unit = new UnitReq();
            unit.setUnitName(name);
            unit.setUnitNature(UnitNatureEnum.valueOf(input.nature()));
            unit.setUnitType(UnitTypeEnum.CUSTOMER);
            unit.setStatus(DisEnableStatusEnum.ENABLE);
            return units.create(unit).toString();
        });
        var rows = customers.fieldCustomers(query(Long.valueOf(id)));
        if (rows.isEmpty()) throw new ServiceException("当前无法使用此客户档案，请联系管理员");
        return rows.get(0);
    }

    private RepairApply requireCustomer(Long id) {
        var query = query(id);
        if (customers.fieldCustomers(query).isEmpty()) throw new ServiceException("客户不存在、已停用或超出服务范围");
        return query;
    }

    public List<Map<String,String>> openRepairs(Long customerId) {
        var query = requireCustomer(customerId);
        query.getParams().put("actor", UserContextHolder.getUserId());
        return customers.fieldOpenRepairs(query);
    }

    @Transactional(rollbackFor=Exception.class)
    public String submit(Request input) {
        var scope = requireCustomer(input.customerId());
        var tenant = scope.getTenantId();
        var actor = UserContextHolder.getUserId();
        return idempotency.execute(tenant, "engineer-field-" + actor, input.requestId().toString(), () -> {
            var apply = new RepairApply();
            apply.setTenantId(tenant);
            apply.setCustomerId(input.customerId());
            apply.setContactName(input.contactName().trim());
            apply.setContactPhone(input.contactPhone().trim());
            apply.setReportAddress(input.address().trim());
            apply.setFaultDesc(input.fault().trim());
            apply.setRemark(input.equipment());
            apply.setSource("ENGINEER_PHONE");
            apply.setCreateBy(UserContextHolder.getUsername());
            apply.setReporterName(UserContextHolder.getNickname());
            apply.setPhotos(attachments.fieldRepairPhotos(input.photos(), actor));
            if (applies.insertRepairApply(apply) != 1) throw new ServiceException("现场报修保存失败");
            return pool.acceptApply(tenant, apply, List.of(), actor, apply.getCreateBy()).toString();
        });
    }
}
