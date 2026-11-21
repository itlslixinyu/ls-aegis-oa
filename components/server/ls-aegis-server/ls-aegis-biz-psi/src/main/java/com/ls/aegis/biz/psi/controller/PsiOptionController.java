package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.biz.psi.mapper.CompanyAccountMapper;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.mapper.StockBatchMapper;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.model.entity.CompanyAccountDO;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.entity.StockBatchDO;
import com.ls.aegis.biz.psi.model.entity.UnitDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.resp.LabelValueResp;
import com.ls.aegis.biz.psi.support.AccountNoMask;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.web.model.R;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主数据下拉选项
 */
@Tag(name = "PSI 下拉选项 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/options")
public class PsiOptionController {

    private final PsiWarehouseMapper warehouseMapper;
    private final GoodsMapper goodsMapper;
    private final StockBatchMapper stockBatchMapper;
    private final UnitMapper unitMapper;
    private final CompanyAccountMapper companyAccountMapper;

    @Operation(summary = "仓库下拉")
    @SaCheckPermission(value = {
        "psi:warehouse:list",
        "psi:stockFlow:list",
        "psi:stockQuery:list",
        "psi:check:list", "psi:check:create",
        "quote:query", "quote:list", "quote:add", "quote:update",
    }, mode = SaMode.OR)
    @GetMapping("/warehouses")
    public R<List<LabelValueResp>> warehouses() {
        var q = Wrappers.lambdaQuery(WarehouseDO.class)
            .eq(WarehouseDO::getStatus, DisEnableStatusEnum.ENABLE)
            .orderByDesc(WarehouseDO::getIsDefault)
            .orderByAsc(WarehouseDO::getId);
        // 非超管/租户管理员只能选自己负责的仓库
        if (!UserContextHolder.isSuperAdmin() && !UserContextHolder.isTenantAdmin()) {
            q.eq(WarehouseDO::getManagerUserId, UserContextHolder.getUserId());
        }
        List<WarehouseDO> list = warehouseMapper.selectList(q);
        return R.ok(list.stream()
            .map(w -> new LabelValueResp(w.getWarehouseName() + "（" + w.getWarehouseCode() + "）", w.getId()))
            .toList());
    }

    @Operation(summary = "商品下拉（可按仓库过滤库存商品；stockAcrossWarehouses=跨全部可见仓有库存商品）")
    @SaCheckPermission(value = {
        "psi:goods:list",
        "psi:stockFlow:list",
        "psi:stockQuery:list",
        "psi:check:list", "psi:check:create",
        "quote:query", "quote:list", "quote:add", "quote:update",
    }, mode = SaMode.OR)
    @GetMapping("/goods")
    public R<List<LabelValueResp>> goods(
        @RequestParam(required = false) Long warehouseId,
        @RequestParam(required = false, defaultValue = "false") boolean stockAcrossWarehouses) {
        if (warehouseId != null) {
            assertWarehouseAccess(warehouseId);
            return R.ok(buildStockGoodsOptions(List.of(warehouseId)));
        }
        if (stockAcrossWarehouses) {
            List<Long> warehouseIds = listAccessibleWarehouseIds();
            if (warehouseIds.isEmpty()) {
                return R.ok(List.of());
            }
            return R.ok(buildStockGoodsOptions(warehouseIds));
        }
        List<GoodsDO> list = goodsMapper.selectList(Wrappers.lambdaQuery(GoodsDO.class)
            .eq(GoodsDO::getStatus, DisEnableStatusEnum.ENABLE)
            .orderByAsc(GoodsDO::getId));
        return R.ok(list.stream()
            .map(g -> new LabelValueResp(g.getGoodsName() + "（" + g.getGoodsCode() + "）", g.getId()))
            .toList());
    }

    /** 可见仓库：超管/租户管理员全部启用仓，其余仅自己负责的仓 */
    private List<Long> listAccessibleWarehouseIds() {
        var q = Wrappers.lambdaQuery(WarehouseDO.class)
            .eq(WarehouseDO::getStatus, DisEnableStatusEnum.ENABLE)
            .select(WarehouseDO::getId);
        if (!UserContextHolder.isSuperAdmin() && !UserContextHolder.isTenantAdmin()) {
            q.eq(WarehouseDO::getManagerUserId, UserContextHolder.getUserId());
        }
        return warehouseMapper.selectList(q).stream().map(WarehouseDO::getId).toList();
    }

    /** 指定仓库集合内 remain>0 的商品，附汇总库存与加权未税单价 */
    private List<LabelValueResp> buildStockGoodsOptions(List<Long> warehouseIds) {
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return List.of();
        }
        List<StockBatchDO> batches = stockBatchMapper.selectList(Wrappers.lambdaQuery(StockBatchDO.class)
            .in(StockBatchDO::getWarehouseId, warehouseIds)
            .gt(StockBatchDO::getRemainQuantity, BigDecimal.ZERO)
            .select(StockBatchDO::getGoodsId, StockBatchDO::getRemainQuantity, StockBatchDO::getRemainAmount));
        Map<Long, BigDecimal> qtyByGoods = new HashMap<>();
        Map<Long, BigDecimal> amtByGoods = new HashMap<>();
        for (StockBatchDO b : batches) {
            if (b.getGoodsId() == null) {
                continue;
            }
            qtyByGoods.merge(b.getGoodsId(), nz(b.getRemainQuantity()), BigDecimal::add);
            amtByGoods.merge(b.getGoodsId(), nz(b.getRemainAmount()), BigDecimal::add);
        }
        if (qtyByGoods.isEmpty()) {
            return List.of();
        }
        List<GoodsDO> list = goodsMapper.selectList(Wrappers.lambdaQuery(GoodsDO.class)
            .eq(GoodsDO::getStatus, DisEnableStatusEnum.ENABLE)
            .in(GoodsDO::getId, qtyByGoods.keySet())
            .orderByAsc(GoodsDO::getId));
        return list.stream()
            .map(g -> {
                BigDecimal qty = qtyByGoods.getOrDefault(g.getId(), BigDecimal.ZERO);
                BigDecimal amt = amtByGoods.getOrDefault(g.getId(), BigDecimal.ZERO);
                BigDecimal avgPrice = BigDecimal.ZERO;
                if (qty.compareTo(BigDecimal.ZERO) > 0 && amt.compareTo(BigDecimal.ZERO) > 0) {
                    avgPrice = amt.divide(qty, 6, java.math.RoundingMode.HALF_UP);
                } else if (g.getPurchasePrice() != null && g.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
                    avgPrice = g.getPurchasePrice().setScale(6, java.math.RoundingMode.HALF_UP);
                }
                String qtyText = qty.stripTrailingZeros().toPlainString();
                return new LabelValueResp(
                    g.getGoodsName() + "（" + g.getGoodsCode() + "）库存" + qtyText,
                    g.getId(),
                    qty,
                    avgPrice);
            })
            .toList();
    }

    @Operation(summary = "往来单位下拉")
    @SaCheckPermission("psi:unit:list")
    @GetMapping("/units")
    public R<List<LabelValueResp>> units(@RequestParam(required = false) String unitType) {
        var q = Wrappers.lambdaQuery(UnitDO.class).eq(UnitDO::getStatus, DisEnableStatusEnum.ENABLE);
        if ("CUSTOMER".equalsIgnoreCase(unitType)) {
            q.and(w -> w.eq(UnitDO::getUnitType, UnitTypeEnum.CUSTOMER).or().eq(UnitDO::getUnitType, UnitTypeEnum.BOTH));
        } else if ("SUPPLIER".equalsIgnoreCase(unitType)) {
            q.and(w -> w.eq(UnitDO::getUnitType, UnitTypeEnum.SUPPLIER).or().eq(UnitDO::getUnitType, UnitTypeEnum.BOTH));
        }
        q.orderByAsc(UnitDO::getId);
        List<UnitDO> list = unitMapper.selectList(q);
        return R.ok(list.stream()
            .map(u -> new LabelValueResp(u.getUnitName() + "（" + u.getUnitCode() + "）", u.getId()))
            .toList());
    }

    @Operation(summary = "银行账户下拉")
    @SaCheckPermission("psi:account:list")
    @GetMapping("/accounts")
    public R<List<LabelValueResp>> accounts() {
        List<CompanyAccountDO> list = companyAccountMapper.selectList(Wrappers.lambdaQuery(CompanyAccountDO.class)
            .eq(CompanyAccountDO::getStatus, DisEnableStatusEnum.ENABLE)
            .orderByAsc(CompanyAccountDO::getId));
        return R.ok(list.stream()
            .map(a -> new LabelValueResp(a.getAccountName() + "（" + AccountNoMask.mask(a.getAccountNo()) + "）", a.getId()))
            .toList());
    }

    private void assertWarehouseAccess(Long warehouseId) {
        WarehouseDO wh = warehouseMapper.selectById(warehouseId);
        CheckUtils.throwIfNull(wh, "仓库不存在");
        if (UserContextHolder.isSuperAdmin() || UserContextHolder.isTenantAdmin()) {
            return;
        }
        CheckUtils.throwIf(
            !ObjectUtil.equal(wh.getManagerUserId(), UserContextHolder.getUserId()),
            "只能查看自己负责的仓库");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
