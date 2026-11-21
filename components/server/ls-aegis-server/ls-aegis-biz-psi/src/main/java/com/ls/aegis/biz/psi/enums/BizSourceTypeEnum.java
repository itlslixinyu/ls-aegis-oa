package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 业务来源类型
 */
@Getter
@RequiredArgsConstructor
public enum BizSourceTypeEnum implements BaseEnum<String> {

    STOCK_IN("STOCK_IN", "采购入库"),
    STOCK_OUT("STOCK_OUT", "销售出库"),
    REPAIR_REVERSE("REPAIR_REVERSE", "维修出库冲销"),
    SALE_RETURN("SALE_RETURN", "销售退货"),
    PURCHASE_RETURN("PURCHASE_RETURN", "采购退货"),
    INIT_AR("INIT_AR", "应收期初"),
    INIT_AP("INIT_AP", "应付期初"),
    INIT_STOCK("INIT_STOCK", "期初库存"),
    INIT_PREPAY("INIT_PREPAY", "预付期初"),
    TRANSFER("TRANSFER", "仓间调拨"),
    STOCK_CHECK("STOCK_CHECK", "盘点"),
    STOCK_ADJUST("STOCK_ADJUST", "库存调整"),
    RECEIPT("RECEIPT", "回款"),
    PAYMENT("PAYMENT", "付款"),
    INVOICE_OUT("INVOICE_OUT", "销项发票"),
    INVOICE_IN("INVOICE_IN", "进项发票"),
    CONTRACT_SALE("CONTRACT_SALE", "销售合同"),
    CONTRACT_PURCHASE("CONTRACT_PURCHASE", "采购合同");

    private final String value;
    private final String description;
}
