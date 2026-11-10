package com.ls.aegis.biz.eoms.quote.bridge;

import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.biz.quote.port.QuoteToRepairBridge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 报价转维修工单桥接：包装原 convertToOrder 中的建单逻辑
 */
@Component
@RequiredArgsConstructor
public class EomsQuoteToRepairBridge implements QuoteToRepairBridge {

    private final IRepairOrderService repairOrderService;

    @Override
    public Long createOrderFromQuote(Long quoteId, Long customerId, String quoteNo, String address, String remark,
        String operator) {
        RepairOrder order = new RepairOrder();
        order.setCustomerId(customerId);
        order.setQuoteId(quoteId);
        order.setAddress(address);
        order.setCreateBy(operator);
        if (StringUtils.isNotEmpty(remark)) {
            order.setRemark(remark);
        } else {
            order.setRemark("报价单[" + quoteNo + "]转工单");
        }
        repairOrderService.insertRepairOrder(order);
        return order.getId();
    }
}
