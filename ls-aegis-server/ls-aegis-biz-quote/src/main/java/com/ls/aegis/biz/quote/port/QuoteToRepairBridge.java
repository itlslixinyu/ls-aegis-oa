package com.ls.aegis.biz.quote.port;

/**
 * 报价转维修工单端口（由运维报修模块实现）
 */
public interface QuoteToRepairBridge {

    /**
     * 根据报价创建维修工单
     *
     * @param quoteId    报价单 ID
     * @param customerId 客户 ID
     * @param quoteNo    报价编号（用于备注）
     * @param address    维修地址（可选）
     * @param remark     备注（可选）
     * @param operator   操作人
     * @return 新工单 ID
     */
    Long createOrderFromQuote(Long quoteId, Long customerId, String quoteNo, String address, String remark,
        String operator);
}
