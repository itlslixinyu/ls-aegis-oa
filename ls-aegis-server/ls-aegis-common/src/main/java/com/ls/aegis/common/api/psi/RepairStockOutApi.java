package com.ls.aegis.common.api.psi;

import java.math.BigDecimal;
import java.util.List;

/**
 * 维修结算出库（由 PSI 实现；EOMS 勿直接依赖 biz-psi）
 */
public interface RepairStockOutApi {

    /**
     * 按工单用料生成 REPAIR 出库单并确认扣减 FIFO 库存。同一工单/结算幂等；返回已确认出库链的实际FIFO材料成本。
     */
    BigDecimal confirmRepairOut(Cmd cmd);

    /** 冲销指定工单的维修出库；原路恢复FIFO批次，保留正反审计，重复调用无新增影响。 */
    void reverseRepairOut(Long orderId, String orderNo);

    /** 已授权工单撤销关闭前的内部完整性断言；有任意非VOID维修出库则拒绝。 */
    void assertNoActiveRepairOut(Long orderId, String orderNo);

    class Cmd {
        private Long orderId;
        private Long settlementId;
        private String orderNo;
        private Long customerId;
        private List<Item> items;

        public Long getSettlementId() { return settlementId; }

        public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public List<Item> getItems() {
            return items;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }
    }

    class Item {
        private Long goodsId;
        private String goodsName;
        private BigDecimal quantity;
        private BigDecimal taxInclusivePrice;
        private Long warehouseId;

        public Long getGoodsId() {
            return goodsId;
        }

        public void setGoodsId(Long goodsId) {
            this.goodsId = goodsId;
        }

        public String getGoodsName() {
            return goodsName;
        }

        public void setGoodsName(String goodsName) {
            this.goodsName = goodsName;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getTaxInclusivePrice() {
            return taxInclusivePrice;
        }

        public void setTaxInclusivePrice(BigDecimal taxInclusivePrice) {
            this.taxInclusivePrice = taxInclusivePrice;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }
    }
}
