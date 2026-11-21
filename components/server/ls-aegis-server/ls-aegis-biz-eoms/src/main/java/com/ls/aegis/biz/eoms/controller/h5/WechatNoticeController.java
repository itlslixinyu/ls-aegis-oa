package com.ls.aegis.biz.eoms.controller.h5;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.H5CustomerInfo;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.interceptor.H5TokenInterceptor;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.biz.eoms.service.h5.H5CustomerScope;
import com.ls.aegis.biz.eoms.service.h5.WechatNoticeService;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.compat.common.exception.ServiceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;

import java.util.Objects;
import java.util.Set;

@Log(ignore = true)
@RestController
@RequiredArgsConstructor
public class WechatNoticeController {
    private final H5CustomerScope customerScope;
    private final RepairOrderMapper orders;
    private final WechatNoticeService notices;

    @SaIgnore
    @PostMapping("/eoms/h5/repair/{applyId}/wechat-subscription")
    public R<WechatNoticeService.Ticket> customerTicket(
        @RequestAttribute(H5TokenInterceptor.ATTR_CUSTOMER_INFO) H5CustomerInfo customer, @PathVariable Long applyId) {
        var apply = customerScope.requireApply(customer, applyId);
        if (!Objects.equals(customer.getAccountId(), apply.getReporterAccountId())) {
            throw new ServiceException("请由报修提交人订阅通知");
        }
        var query = new RepairOrder();
        query.setApplyId(applyId);
        query.setCustomerId(customer.getCustomerId());
        var completed = Set.of(EomsConstants.ORDER_STATUS_WAIT_SETTLEMENT, EomsConstants.ORDER_STATUS_FINISHED,
            EomsConstants.ORDER_STATUS_CLOSED);
        if (orders.selectRepairOrderList(query).stream().anyMatch(order ->
            Objects.equals(order.getTenantId(), customer.getTenantId()) && completed.contains(order.getStatus()))) {
            throw new ServiceException("维修已完成");
        }
        return R.ok(notices.issueCustomer(customer.getTenantId(), customer.getCustomerId(), customer.getAccountId(), applyId));
    }

    @SaCheckPermission("eoms:order:query")
    @PostMapping("/eoms/engineer/wechat-subscription")
    public R<WechatNoticeService.Ticket> engineerTicket() {
        Long tenant = Objects.requireNonNullElse(UserContextHolder.getTenantId(), 0L);
        Long engineer = UserContextHolder.getUserId();
        if (engineer == null || orders.countEnabledEngineer(tenant, engineer) != 1) {
            throw new ServiceException("当前账号不能订阅工程师通知");
        }
        return R.ok(notices.issueEngineer(tenant, engineer));
    }

    @SaIgnore
    @PostMapping("/eoms/h5/auth/wechat-subscription")
    public R<Void> redeem(@Valid @RequestBody Redeem body) {
        notices.redeem(body.ticket(), body.code());
        return R.ok();
    }

    public record Redeem(@NotBlank @Pattern(regexp = "[0-9a-f-]{36}") String ticket,
                         @NotBlank @Size(max = 128) String code) {}
}
