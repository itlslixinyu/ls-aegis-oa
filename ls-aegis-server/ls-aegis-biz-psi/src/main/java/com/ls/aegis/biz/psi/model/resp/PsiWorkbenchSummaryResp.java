package com.ls.aegis.biz.psi.model.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * PSI 工作台摘要（资金 + 大跨度 + 库存）
 */
@Data
public class PsiWorkbenchSummaryResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 未完成待办数 */
    private long openTodoCount;
    /** 未结清应收开口笔数 */
    private long openReceivableCount;
    /** 未结清应收未核销合计 */
    private BigDecimal openReceivableAmount = BigDecimal.ZERO;
    /** 未结清应付开口笔数 */
    private long openPayableCount;
    /** 未结清应付未核销合计 */
    private BigDecimal openPayableAmount = BigDecimal.ZERO;
    /** 最近未完成待办（最多 8 条） */
    private List<PsiTodoResp> recentTodos = new ArrayList<>();
    /** 库存上下限预警（最多 20 条） */
    private List<PsiStockAlertResp> stockAlerts = new ArrayList<>();

    /** 草稿回款笔数（待确认） */
    private long pendingReceiptCount;
    /** 草稿回款金额合计 */
    private BigDecimal pendingReceiptAmount = BigDecimal.ZERO;
    /** 未匹配银行流水笔数 */
    private long unmatchedBankCount;
    /** 未匹配银行流水金额合计 */
    private BigDecimal unmatchedBankAmount = BigDecimal.ZERO;

    /** 本月已确认出库（销售）笔数 */
    private long monthSaleCount;
    /** 本月已确认出库含税合计 */
    private BigDecimal monthSaleAmount = BigDecimal.ZERO;
    /** 本月已确认出库数量合计 */
    private BigDecimal monthSaleQty = BigDecimal.ZERO;
    /** 本月已确认入库（采购）笔数 */
    private long monthPurchaseCount;
    /** 本月已确认入库含税合计 */
    private BigDecimal monthPurchaseAmount = BigDecimal.ZERO;
    /** 本月已确认入库数量合计 */
    private BigDecimal monthPurchaseQty = BigDecimal.ZERO;

    /** 近 30 天已确认入库笔数 */
    private long last30InCount;
    /** 近 30 天已确认入库含税合计 */
    private BigDecimal last30InAmount = BigDecimal.ZERO;
    /** 近 30 天已确认入库数量合计 */
    private BigDecimal last30InQty = BigDecimal.ZERO;
    /** 近 30 天已确认出库笔数 */
    private long last30OutCount;
    /** 近 30 天已确认出库含税合计 */
    private BigDecimal last30OutAmount = BigDecimal.ZERO;
    /** 近 30 天已确认出库数量合计 */
    private BigDecimal last30OutQty = BigDecimal.ZERO;
}
