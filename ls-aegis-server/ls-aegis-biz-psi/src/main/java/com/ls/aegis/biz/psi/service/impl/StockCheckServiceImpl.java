package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.mapper.StockBatchMapper;
import com.ls.aegis.biz.psi.mapper.StockCheckItemMapper;
import com.ls.aegis.biz.psi.mapper.StockCheckMapper;
import com.ls.aegis.biz.psi.mapper.StockFlowMapper;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.entity.StockBatchDO;
import com.ls.aegis.biz.psi.model.entity.StockCheckDO;
import com.ls.aegis.biz.psi.model.entity.StockCheckItemDO;
import com.ls.aegis.biz.psi.model.entity.StockFlowDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.query.StockCheckQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockCheckImportRowReq;
import com.ls.aegis.biz.psi.model.req.StockCheckReq;
import com.ls.aegis.biz.psi.model.resp.StockCheckBookInfoResp;
import com.ls.aegis.biz.psi.model.resp.StockCheckImportParseResp;
import com.ls.aegis.biz.psi.model.resp.StockCheckResp;
import com.ls.aegis.biz.psi.service.StockCheckService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.FifoStockHelper;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.biz.psi.support.PsiImportHelper;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StockCheckServiceImpl implements StockCheckService {

    private final StockCheckMapper stockCheckMapper;
    private final StockCheckItemMapper stockCheckItemMapper;
    private final StockBatchMapper stockBatchMapper;
    private final StockFlowMapper stockFlowMapper;
    private final PsiWarehouseMapper warehouseMapper;
    private final GoodsMapper goodsMapper;
    private final DocNoGenerator docNoGenerator;
    private final FifoStockHelper fifoStockHelper;
    private final IdempotentHelper idempotentHelper;

    @Override
    public PageResp<StockCheckResp> page(StockCheckQuery query, PageQuery pageQuery) {
        QueryWrapper<StockCheckDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<StockCheckDO> page = stockCheckMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<StockCheckResp> resp = PageResp.build(page, StockCheckResp.class);
        fillWarehouseNames(resp.getList());
        return resp;
    }

    @Override
    public StockCheckResp get(Long id) {
        StockCheckDO entity = require(id);
        StockCheckResp resp = BeanUtil.copyProperties(entity, StockCheckResp.class);
        List<StockCheckItemDO> items = stockCheckItemMapper.selectList(Wrappers.lambdaQuery(StockCheckItemDO.class)
            .eq(StockCheckItemDO::getCheckId, id).orderByAsc(StockCheckItemDO::getSort));
        resp.setItems(BeanUtil.copyToList(items, StockCheckResp.Item.class));
        fillWarehouseNames(List.of(resp));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StockCheckReq req) {
        Long tenantId = TenantFillHelper.currentTenantId();
        StockCheckDO entity = new StockCheckDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_STOCK_CHECK));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setWarehouseId(req.getWarehouseId());
        entity.setCheckDate(req.getCheckDate());
        entity.setRemark(req.getRemark());
        stockCheckMapper.insert(entity);
        insertItems(tenantId, entity.getId(), entity.getWarehouseId(), req.getItems());
        // 录入时写入仓库库存快照版本，确认时比对
        entity.setSnapshotVersion(currentWarehouseStockVersion(tenantId, entity.getWarehouseId()));
        stockCheckMapper.updateById(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StockCheckReq req, Long id) {
        StockCheckDO entity = requireLocked(id);
        assertDraft(entity);
        entity.setWarehouseId(req.getWarehouseId());
        entity.setCheckDate(req.getCheckDate());
        entity.setRemark(req.getRemark());
        stockCheckItemMapper.delete(Wrappers.lambdaQuery(StockCheckItemDO.class).eq(StockCheckItemDO::getCheckId, id));
        insertItems(entity.getTenantId(), id, entity.getWarehouseId(), req.getItems());
        entity.setSnapshotVersion(currentWarehouseStockVersion(entity.getTenantId(), entity.getWarehouseId()));
        stockCheckMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StockCheckDO entity = requireLocked(id);
        assertDraft(entity);
        stockCheckItemMapper.delete(Wrappers.lambdaQuery(StockCheckItemDO.class).eq(StockCheckItemDO::getCheckId, id));
        stockCheckMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirm(Long id, DocConfirmReq req) {
        StockCheckDO locked = stockCheckMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("盘点单不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "stock-check-confirm-" + id, req.getIdempotentKey(), () -> doConfirm(locked, req));
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        PsiImportHelper.downloadTemplate(response, StockCheckImportRowReq.class, "盘点实盘导入模板.xlsx");
    }

    @Override
    public StockCheckImportParseResp parseImportExcel(MultipartFile file, Long warehouseId) {
        CheckUtils.throwIf(file == null || file.isEmpty(), "文件不能为空");
        List<StockCheckImportRowReq> rows;
        try {
            rows = PsiImportHelper.readExcel(file.getInputStream(), StockCheckImportRowReq.class);
        } catch (Exception e) {
            throw new BusinessException("数据文件解析异常，请检查模板格式");
        }
        Long tenantId = TenantFillHelper.currentTenantId();
        if (warehouseId != null) {
            WarehouseDO wh = warehouseMapper.selectById(warehouseId);
            if (wh == null || !ObjectUtil.equal(wh.getTenantId(), tenantId)) {
                throw new BusinessException("仓库不存在");
            }
        }
        StockCheckImportParseResp resp = new StockCheckImportParseResp();
        resp.setTotalRows(rows.size());
        List<StockCheckImportParseResp.Item> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (StockCheckImportRowReq row : rows) {
            if (StrUtil.isBlank(row.getGoodsCode()) && StrUtil.isBlank(row.getCheckQty())) {
                continue;
            }
            CheckUtils.throwIfBlank(row.getGoodsCode(), "商品编号不能为空");
            CheckUtils.throwIfBlank(row.getCheckQty(), "实盘数量不能为空：" + row.getGoodsCode());
            String code = row.getGoodsCode().trim();
            if (!seen.add(code)) {
                throw new BusinessException("商品编号重复：" + code);
            }
            GoodsDO goods = resolveGoods(tenantId, code);
            StockCheckImportParseResp.Item item = new StockCheckImportParseResp.Item();
            item.setGoodsId(goods.getId());
            item.setGoodsCode(goods.getGoodsCode());
            item.setGoodsName(goods.getGoodsName());
            item.setCheckQty(parseNonNegative(row.getCheckQty(), "实盘数量"));
            if (StrUtil.isNotBlank(row.getUntaxPrice())) {
                item.setUntaxPrice(parseNonNegative(row.getUntaxPrice(), "未税单价"));
            } else {
                item.setUntaxPrice(BigDecimal.ZERO);
            }
            if (warehouseId != null) {
                item.setBookQty(nz(stockBatchMapper.sumRemainQuantity(tenantId, warehouseId, goods.getId())));
                if (StrUtil.isBlank(row.getUntaxPrice())) {
                    item.setUntaxPrice(resolveUntaxPrice(tenantId, warehouseId, goods.getId()));
                }
            } else {
                item.setBookQty(BigDecimal.ZERO);
            }
            items.add(item);
        }
        CheckUtils.throwIfEmpty(items, "未解析到有效明细（商品编号、实盘数量必填）");
        resp.setValidRows(items.size());
        resp.setItems(items);
        return resp;
    }

    @Override
    public StockCheckBookInfoResp bookInfo(Long warehouseId, Long goodsId) {
        CheckUtils.throwIfNull(warehouseId, "仓库不能为空");
        CheckUtils.throwIfNull(goodsId, "商品不能为空");
        Long tenantId = TenantFillHelper.currentTenantId();
        WarehouseDO wh = warehouseMapper.selectById(warehouseId);
        if (wh == null || !ObjectUtil.equal(wh.getTenantId(), tenantId)) {
            throw new BusinessException("仓库不存在");
        }
        GoodsDO goods = goodsMapper.selectById(goodsId);
        if (goods == null || !ObjectUtil.equal(goods.getTenantId(), tenantId)) {
            throw new BusinessException("商品不存在");
        }
        StockCheckBookInfoResp resp = new StockCheckBookInfoResp();
        resp.setBookQty(nz(stockBatchMapper.sumRemainQuantity(tenantId, warehouseId, goodsId)));
        resp.setUntaxPrice(resolveUntaxPrice(tenantId, warehouseId, goodsId));
        return resp;
    }

    /**
     * 参考未税单价：剩余金额加权 → 批次单价加权 → 商品采购价
     */
    private BigDecimal resolveUntaxPrice(Long tenantId, Long warehouseId, Long goodsId) {
        BigDecimal qty = nz(stockBatchMapper.sumRemainQuantity(tenantId, warehouseId, goodsId));
        BigDecimal amt = nz(stockBatchMapper.sumRemainAmount(tenantId, warehouseId, goodsId));
        if (qty.compareTo(BigDecimal.ZERO) > 0 && amt.compareTo(BigDecimal.ZERO) > 0) {
            return amt.divide(qty, 6, RoundingMode.HALF_UP);
        }
        List<StockBatchDO> batches = stockBatchMapper.selectList(Wrappers.lambdaQuery(StockBatchDO.class)
            .eq(StockBatchDO::getTenantId, tenantId)
            .eq(StockBatchDO::getWarehouseId, warehouseId)
            .eq(StockBatchDO::getGoodsId, goodsId)
            .gt(StockBatchDO::getRemainQuantity, BigDecimal.ZERO)
            .select(StockBatchDO::getRemainQuantity, StockBatchDO::getUntaxPrice));
        BigDecimal wQty = BigDecimal.ZERO;
        BigDecimal wAmt = BigDecimal.ZERO;
        for (StockBatchDO b : batches) {
            BigDecimal p = nz(b.getUntaxPrice());
            BigDecimal q = nz(b.getRemainQuantity());
            if (p.compareTo(BigDecimal.ZERO) > 0 && q.compareTo(BigDecimal.ZERO) > 0) {
                wQty = wQty.add(q);
                wAmt = wAmt.add(q.multiply(p));
            }
        }
        if (wQty.compareTo(BigDecimal.ZERO) > 0) {
            return wAmt.divide(wQty, 6, RoundingMode.HALF_UP);
        }
        GoodsDO goods = goodsMapper.selectById(goodsId);
        if (goods != null && goods.getPurchasePrice() != null
            && goods.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
            return goods.getPurchasePrice().setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private GoodsDO resolveGoods(Long tenantId, String code) {
        GoodsDO goods = goodsMapper.selectOne(Wrappers.lambdaQuery(GoodsDO.class)
            .eq(GoodsDO::getTenantId, tenantId)
            .eq(GoodsDO::getGoodsCode, code.trim())
            .last("LIMIT 1"));
        if (goods == null) {
            throw new BusinessException("商品编号不存在：" + code);
        }
        return goods;
    }

    private BigDecimal parseNonNegative(String raw, String label) {
        String v = raw.trim().replace(",", "");
        if (!NumberUtil.isNumber(v)) {
            throw new BusinessException(label + "格式不正确：" + raw);
        }
        BigDecimal n = new BigDecimal(v);
        if (n.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(label + "不能为负：" + raw);
        }
        return n;
    }

    private String doConfirm(StockCheckDO locked, DocConfirmReq req) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked);
        Long userId = UserContextHolder.getUserId();
        if (!ObjectUtil.equal(locked.getCreateUser(), userId)) {
            throw new BusinessException("仅制单人可确认生效");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        List<StockCheckItemDO> items = stockCheckItemMapper.selectList(Wrappers.lambdaQuery(StockCheckItemDO.class)
            .eq(StockCheckItemDO::getCheckId, locked.getId()).orderByAsc(StockCheckItemDO::getSort));
        fifoStockHelper.lockAll(items.stream().map(item -> new com.ls.aegis.biz.psi.support.StockMutationLock.Key(
            tenantId, locked.getWarehouseId(), item.getGoodsId())).toList());
        // 快照版本间：null 视为 0，避免旧草稿跳过校验
        Long snap = locked.getSnapshotVersion() == null ? 0L : locked.getSnapshotVersion();
        Long currentVersion = currentWarehouseStockVersion(tenantId, locked.getWarehouseId());
        if (!ObjectUtil.equal(currentVersion, snap)) {
            throw new BusinessException("库存已发生变化（快照版本不一致），请重新录入盘点后再确认");
        }
        if (req.getSnapshotVersion() != null && !ObjectUtil.equal(req.getSnapshotVersion(), snap)) {
            throw new BusinessException("盘点单已被他人修改，请刷新后重试");
        }

        for (StockCheckItemDO item : items) {
            BigDecimal book = fifoStockHelper.currentBalance(tenantId, locked.getWarehouseId(), item.getGoodsId()).quantity();
            if (item.getBookQty() != null && book.compareTo(item.getBookQty()) != 0) {
                throw new BusinessException("账面库存已变化，请重新录入盘点（商品ID=" + item.getGoodsId() + "）");
            }
            BigDecimal diff = item.getCheckQty().subtract(book);
            item.setBookQty(book);
            item.setDiffQty(diff);
            stockCheckItemMapper.updateById(item);
            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                fifoStockHelper.lossOutbound(tenantId, locked.getWarehouseId(), item.getGoodsId(), diff.abs(),
                    BizSourceTypeEnum.STOCK_CHECK, locked.getId(), locked.getDocNo());
            } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                if (item.getUntaxPrice() == null) {
                    throw new BusinessException("盘盈必须填写未税单价，商品ID=" + item.getGoodsId());
                }
                BigDecimal amt = diff.multiply(item.getUntaxPrice()).setScale(2, RoundingMode.HALF_UP);
                fifoStockHelper.profitInbound(tenantId, locked.getWarehouseId(), item.getGoodsId(), diff,
                    item.getUntaxPrice(), amt, locked.getCheckDate(), BizSourceTypeEnum.STOCK_CHECK, locked.getId(), locked.getDocNo());
            }
        }
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(req.getIdempotentKey());
        locked.setConfirmUser(userId);
        locked.setConfirmTime(LocalDateTime.now());
        stockCheckMapper.updateById(locked);
        return locked.getDocNo();
    }

    /** 仓库库存版本：该仓最新一条流水 ID，无流水则为 0 */
    private Long currentWarehouseStockVersion(Long tenantId, Long warehouseId) {
        StockFlowDO last = stockFlowMapper.selectOne(Wrappers.lambdaQuery(StockFlowDO.class)
            .eq(StockFlowDO::getTenantId, tenantId)
            .eq(StockFlowDO::getWarehouseId, warehouseId)
            .orderByDesc(StockFlowDO::getId)
            .last("LIMIT 1"));
        return last == null || last.getId() == null ? 0L : last.getId();
    }

    private void fillWarehouseNames(List<StockCheckResp> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        java.util.Set<Long> ids = list.stream().map(StockCheckResp::getWarehouseId)
            .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        java.util.Map<Long, String> names = new java.util.HashMap<>();
        for (WarehouseDO w : warehouseMapper.selectByIds(ids)) {
            names.put(w.getId(), w.getWarehouseName());
        }
        for (StockCheckResp row : list) {
            if (row.getWarehouseId() != null) {
                row.setWarehouseName(names.get(row.getWarehouseId()));
            }
        }
    }

    private void insertItems(Long tenantId, Long checkId, Long warehouseId, List<StockCheckReq.Item> lines) {
        int sort = 1;
        for (StockCheckReq.Item line : lines) {
            if (line.getCheckQty() == null || line.getCheckQty().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("实盘数量不能为负");
            }
            BigDecimal book = nz(stockBatchMapper.sumRemainQuantity(tenantId, warehouseId, line.getGoodsId()));
            BigDecimal untaxPrice = line.getUntaxPrice();
            if (untaxPrice == null || untaxPrice.compareTo(BigDecimal.ZERO) <= 0) {
                untaxPrice = resolveUntaxPrice(tenantId, warehouseId, line.getGoodsId());
            }
            StockCheckItemDO item = new StockCheckItemDO();
            item.setTenantId(tenantId);
            item.setCheckId(checkId);
            item.setGoodsId(line.getGoodsId());
            item.setBookQty(book);
            item.setCheckQty(line.getCheckQty());
            item.setDiffQty(line.getCheckQty().subtract(book));
            item.setUntaxPrice(untaxPrice);
            item.setSort(sort++);
            stockCheckItemMapper.insert(item);
        }
    }

    private StockCheckDO requireLocked(Long id) {
        StockCheckDO entity = stockCheckMapper.selectByIdForUpdate(id);
        if (entity == null) throw new BusinessException("单据不存在或无权访问");
        return entity;
    }

    private StockCheckDO require(Long id) {
        StockCheckDO entity = stockCheckMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("盘点单不存在");
        }
        return entity;
    }

    private void assertDraft(StockCheckDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿状态允许此操作");
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
