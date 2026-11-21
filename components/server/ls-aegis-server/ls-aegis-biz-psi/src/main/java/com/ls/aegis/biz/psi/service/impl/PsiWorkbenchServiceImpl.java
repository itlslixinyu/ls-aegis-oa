package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.mapper.PsiTodoMapper;
import com.ls.aegis.biz.psi.mapper.StockBatchMapper;
import com.ls.aegis.biz.psi.mapper.PsiWorkbenchMapper;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.entity.PsiTodoDO;
import com.ls.aegis.biz.psi.model.resp.PsiStockAlertResp;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.biz.psi.model.resp.PsiTodoResp;
import com.ls.aegis.biz.psi.model.resp.PsiWorkbenchSummaryResp;
import com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate;
import com.ls.aegis.biz.psi.service.PsiWorkbenchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PsiWorkbenchServiceImpl implements PsiWorkbenchService {

    private final PsiTodoMapper psiTodoMapper;
    private final GoodsMapper goodsMapper;
    private final StockBatchMapper stockBatchMapper;
    private final PsiWorkbenchMapper workbenchMapper;

    @Override
    public PsiWorkbenchSummaryResp summary() {
        validateActor();
        Long tenantId = tenantId();
        PsiWorkbenchSummaryResp resp = new PsiWorkbenchSummaryResp();
        resp.setOpenTodoCount(workbenchMapper.countOpenTodos(tenantId));
        var ar = workbenchMapper.selectReceivables(tenantId);
        resp.setOpenReceivableCount(ar.getCount());
        resp.setOpenReceivableAmount(ar.getAmount());
        var ap = workbenchMapper.selectPayables(tenantId);
        resp.setOpenPayableCount(ap.getCount());
        resp.setOpenPayableAmount(ap.getAmount());
        var receipts = workbenchMapper.selectReceipts(tenantId);
        resp.setPendingReceiptCount(receipts.getCount());
        resp.setPendingReceiptAmount(receipts.getAmount());
        var banks = workbenchMapper.selectBanks(tenantId);
        resp.setUnmatchedBankCount(banks.getCount());
        resp.setUnmatchedBankAmount(banks.getAmount());
        List<PsiTodoDO> todos = psiTodoMapper.selectList(Wrappers.lambdaQuery(PsiTodoDO.class)
            .eq(PsiTodoDO::getDone, false).orderByDesc(PsiTodoDO::getId).last("LIMIT 8"));
        resp.setRecentTodos(todos.stream().map(t -> BeanUtil.copyProperties(t, PsiTodoResp.class)).toList());
        resp.setStockAlerts(buildStockAlerts());
        fillSpanOps(resp, LocalDate.now());
        return resp;
    }

    private void fillSpanOps(PsiWorkbenchSummaryResp resp, LocalDate today) {
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime windowEnd = today.atTime(LocalTime.MAX);
        LocalDateTime spanStart = today.minusDays(29).atStartOfDay();
        LocalDateTime windowStart = monthStart.isBefore(spanStart) ? monthStart : spanStart;
        Long tenantId = tenantId();
        var in = workbenchMapper.selectInHeaders(tenantId, monthStart, spanStart, windowStart, windowEnd);
        var out = workbenchMapper.selectOutHeaders(tenantId, monthStart, spanStart, windowStart, windowEnd);
        var inQty = in.getMonthCount() == 0 && in.getSpanCount() == 0 ? new WorkbenchAggregate()
            : workbenchMapper.selectInQuantities(tenantId, monthStart, spanStart, windowStart, windowEnd);
        var outQty = out.getMonthCount() == 0 && out.getSpanCount() == 0 ? new WorkbenchAggregate()
            : workbenchMapper.selectOutQuantities(tenantId, monthStart, spanStart, windowStart, windowEnd);
        resp.setMonthPurchaseCount(in.getMonthCount());
        resp.setMonthPurchaseAmount(in.getMonthAmount());
        resp.setLast30InCount(in.getSpanCount());
        resp.setLast30InAmount(in.getSpanAmount());
        resp.setMonthSaleCount(out.getMonthCount());
        resp.setMonthSaleAmount(out.getMonthAmount());
        resp.setLast30OutCount(out.getSpanCount());
        resp.setLast30OutAmount(out.getSpanAmount());
        resp.setMonthPurchaseQty(inQty.getMonthQty());
        resp.setLast30InQty(inQty.getSpanQty());
        resp.setMonthSaleQty(outQty.getMonthQty());
        resp.setLast30OutQty(outQty.getSpanQty());
    }

    private Long tenantId() {
        return UserContextHolder.getContext().getTenantId() == null ? 0L : UserContextHolder.getContext().getTenantId();
    }

    private void validateActor() {
        cn.dev33.satoken.stp.StpUtil.checkLogin();
        var actor = UserContextHolder.getContext();
        if (actor == null || actor.getId() == null || (!actor.isSuperAdmin() && !actor.isTenantAdmin()
                && (actor.getDeptId() == null || actor.getRoles() == null || actor.getRoles().isEmpty()
                || actor.getRoles().stream().anyMatch(role -> role == null || role.getId() == null || role.getDataScope() == null)))) {
            throw new top.continew.starter.core.exception.BusinessException("工作台数据权限上下文无效，请重新登录");
        }
    }

    private List<PsiStockAlertResp> buildStockAlerts() {
        validateActor();
        var actor = UserContextHolder.getContext();
        List<GoodsDO> watched = goodsMapper.selectList(Wrappers.lambdaQuery(GoodsDO.class)
            .and(w -> w.isNotNull(GoodsDO::getStockLower).or().isNotNull(GoodsDO::getStockUpper))
            .select(GoodsDO::getId, GoodsDO::getGoodsCode, GoodsDO::getGoodsName, GoodsDO::getStockLower, GoodsDO::getStockUpper)
            .last("LIMIT 500"));
        if (watched.isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> qtyByGoods = new HashMap<>();
        var quantities = stockBatchMapper.selectAlertQuantities(actor.getTenantId() == null ? 0L : actor.getTenantId(),
            watched.stream().map(GoodsDO::getId).toList());
        for (var quantity : quantities) {
            qtyByGoods.put(quantity.getGoodsId(), nz(quantity.getRemainQuantity()));
        }
        List<PsiStockAlertResp> alerts = new ArrayList<>();
        for (GoodsDO g : watched) {
            BigDecimal qty = qtyByGoods.getOrDefault(g.getId(), BigDecimal.ZERO);
            String type = null;
            if (g.getStockLower() != null && qty.compareTo(g.getStockLower()) < 0) {
                type = "LOW";
            } else if (g.getStockUpper() != null && qty.compareTo(g.getStockUpper()) > 0) {
                type = "HIGH";
            }
            if (type == null) {
                continue;
            }
            PsiStockAlertResp row = new PsiStockAlertResp();
            row.setGoodsId(g.getId());
            row.setGoodsCode(g.getGoodsCode());
            row.setGoodsName(g.getGoodsName());
            row.setStockQty(qty);
            row.setStockLower(g.getStockLower());
            row.setStockUpper(g.getStockUpper());
            row.setAlertType(type);
            alerts.add(row);
            if (alerts.size() >= 20) {
                break;
            }
        }
        return alerts;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
