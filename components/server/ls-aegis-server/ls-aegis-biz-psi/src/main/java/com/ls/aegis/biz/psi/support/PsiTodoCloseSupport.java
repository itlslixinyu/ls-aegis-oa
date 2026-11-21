package com.ls.aegis.biz.psi.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.mapper.StockReturnMapper;
import com.ls.aegis.biz.psi.model.entity.PayableDO;
import com.ls.aegis.biz.psi.model.entity.ReceivableDO;
import com.ls.aegis.biz.psi.model.entity.StockReturnDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 业务动作闭环待办（冲红 / 反核销）
 */
@Component
@RequiredArgsConstructor
public class PsiTodoCloseSupport {

    private final StockReturnMapper stockReturnMapper;
    private final PsiTodoHelper psiTodoHelper;

    public void completeRedByStockOut(Long stockOutId) {
        if (stockOutId == null) {
            return;
        }
        for (StockReturnDO r : returnsByOut(stockOutId)) {
            psiTodoHelper.completeOpen("RED_INVOICE", r.getDocNo());
        }
    }

    public void completeRedByStockIn(Long stockInId) {
        if (stockInId == null) {
            return;
        }
        for (StockReturnDO r : returnsByIn(stockInId)) {
            psiTodoHelper.completeOpen("RED_INVOICE", r.getDocNo());
        }
    }

    /** 开口已核销额归零时，关闭对应退货的「待反核销」 */
    public void completeReverseIfCleared(ReceivableDO ar) {
        if (ar == null || ar.getSourceId() == null) {
            return;
        }
        if (nz(ar.getWrittenOffAmount()).compareTo(BigDecimal.ZERO) > 0) {
            return;
        }
        for (StockReturnDO r : returnsByOut(ar.getSourceId())) {
            psiTodoHelper.completeOpen("REVERSE_WRITEOFF", r.getDocNo());
        }
    }

    public void completeReverseIfCleared(PayableDO ap) {
        if (ap == null || ap.getSourceId() == null) {
            return;
        }
        if (nz(ap.getWrittenOffAmount()).compareTo(BigDecimal.ZERO) > 0) {
            return;
        }
        for (StockReturnDO r : returnsByIn(ap.getSourceId())) {
            psiTodoHelper.completeOpen("REVERSE_WRITEOFF", r.getDocNo());
        }
    }

    private List<StockReturnDO> returnsByOut(Long stockOutId) {
        return stockReturnMapper.selectList(Wrappers.lambdaQuery(StockReturnDO.class)
            .eq(StockReturnDO::getOriginStockOutId, stockOutId));
    }

    private List<StockReturnDO> returnsByIn(Long stockInId) {
        return stockReturnMapper.selectList(Wrappers.lambdaQuery(StockReturnDO.class)
            .eq(StockReturnDO::getOriginStockInId, stockInId));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
