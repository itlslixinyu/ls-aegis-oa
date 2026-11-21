package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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
import com.ls.aegis.biz.psi.mapper.InitStockItemMapper;
import com.ls.aegis.biz.psi.mapper.InitStockMapper;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.entity.InitStockDO;
import com.ls.aegis.biz.psi.model.entity.InitStockItemDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.query.InitStockQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InitStockImportRowReq;
import com.ls.aegis.biz.psi.model.req.InitStockReq;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.resp.InitStockResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.service.InitStockService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.FifoStockHelper;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.biz.psi.support.PsiImportHelper;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InitStockServiceImpl implements InitStockService {

    private final InitStockMapper initStockMapper;
    private final InitStockItemMapper initStockItemMapper;
    private final PsiWarehouseMapper warehouseMapper;
    private final GoodsMapper goodsMapper;
    private final DocNoGenerator docNoGenerator;
    private final FifoStockHelper fifoStockHelper;
    private final IdempotentHelper idempotentHelper;

    @Override
    public PageResp<InitStockResp> page(InitStockQuery query, PageQuery pageQuery) {
        QueryWrapper<InitStockDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<InitStockDO> page = initStockMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<InitStockResp> resp = PageResp.build(page, InitStockResp.class);
        fillWarehouseNames(resp.getList());
        return resp;
    }

    @Override
    public InitStockResp get(Long id) {
        InitStockDO entity = require(id);
        InitStockResp resp = BeanUtil.copyProperties(entity, InitStockResp.class);
        List<InitStockItemDO> items = initStockItemMapper.selectList(Wrappers.lambdaQuery(InitStockItemDO.class)
            .eq(InitStockItemDO::getInitId, id)
            .orderByAsc(InitStockItemDO::getSort));
        resp.setItems(BeanUtil.copyToList(items, InitStockResp.Item.class));
        fillWarehouseNames(List.of(resp));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(InitStockReq req) {
        Long tenantId = TenantFillHelper.currentTenantId();
        InitStockDO entity = new InitStockDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_INIT_STOCK));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setInitDate(req.getInitDate());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setRemark(req.getRemark());
        initStockMapper.insert(entity);
        saveItems(tenantId, entity.getId(), req.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(InitStockReq req, Long id) {
        InitStockDO entity = require(id);
        assertDraft(entity);
        entity.setInitDate(req.getInitDate());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setRemark(req.getRemark());
        initStockMapper.updateById(entity);
        initStockItemMapper.delete(Wrappers.lambdaQuery(InitStockItemDO.class).eq(InitStockItemDO::getInitId, id));
        saveItems(entity.getTenantId(), id, req.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        InitStockDO entity = require(id);
        assertDraft(entity);
        initStockItemMapper.delete(Wrappers.lambdaQuery(InitStockItemDO.class).eq(InitStockItemDO::getInitId, id));
        initStockMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirm(Long id, DocConfirmReq req) {
        InitStockDO locked = initStockMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("期初单不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "init-stock-confirm-" + id, req.getIdempotentKey(), () -> doConfirm(locked, req
            .getIdempotentKey()));
    }

    private String doConfirm(InitStockDO locked, String idempotentKey) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked);
        Long userId = UserContextHolder.getUserId();
        if (!ObjectUtil.equal(locked.getCreateUser(), userId)) {
            throw new BusinessException("仅制单人可确认生效");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        List<InitStockItemDO> items = initStockItemMapper.selectList(Wrappers.lambdaQuery(InitStockItemDO.class)
            .eq(InitStockItemDO::getInitId, locked.getId())
            .orderByAsc(InitStockItemDO::getSort));
        fifoStockHelper.lockAll(items.stream().map(item -> new com.ls.aegis.biz.psi.support.StockMutationLock.Key(
            tenantId, locked.getWarehouseId(), item.getGoodsId())).toList());
        for (InitStockItemDO item : items) {
            fifoStockHelper.inbound(tenantId, locked.getWarehouseId(), item.getGoodsId(), item.getQuantity(), item
                .getUntaxPrice(), item.getUntaxAmount(), locked.getInitDate(), BizSourceTypeEnum.INIT_STOCK, locked
                    .getId(), locked.getDocNo());
        }
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(idempotentKey);
        locked.setConfirmUser(userId);
        locked.setConfirmTime(LocalDateTime.now());
        initStockMapper.updateById(locked);
        return locked.getDocNo();
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        PsiImportHelper.downloadTemplate(response, InitStockImportRowReq.class, "库存期初导入模板.xlsx");
    }

    @Override
    public PsiImportParseResp parseImport(MultipartFile file) {
        CheckUtils.throwIf(file == null || file.isEmpty(), "文件不能为空");
        List<InitStockImportRowReq> rows;
        try {
            rows = PsiImportHelper.readExcel(file.getInputStream(), InitStockImportRowReq.class);
        } catch (Exception e) {
            throw new BusinessException("数据文件解析异常，请检查模板格式");
        }
        PsiImportParseResp resp = new PsiImportParseResp();
        resp.setTotalRows(rows.size());
        Long tenantId = TenantFillHelper.currentTenantId();
        List<InitStockImportRowReq> valid = new ArrayList<>();
        for (InitStockImportRowReq row : rows) {
            if (StrUtil.isBlank(row.getWarehouseCode()) && StrUtil.isBlank(row.getGoodsCode())) {
                continue;
            }
            CheckUtils.throwIfBlank(row.getWarehouseCode(), "仓库编码不能为空");
            CheckUtils.throwIfBlank(row.getGoodsCode(), "商品编号不能为空");
            CheckUtils.throwIfBlank(row.getQuantity(), "数量不能为空：" + row.getGoodsCode());
            CheckUtils.throwIfBlank(row.getUntaxPrice(), "未税单价不能为空：" + row.getGoodsCode());
            resolveWarehouse(tenantId, row.getWarehouseCode());
            resolveGoods(tenantId, row.getGoodsCode());
            parseDecimal(row.getQuantity(), "数量");
            parseDecimal(row.getUntaxPrice(), "未税单价");
            if (StrUtil.isNotBlank(row.getInitDate())) {
                parseInitDate(row.getInitDate());
            }
            valid.add(row);
        }
        CheckUtils.throwIfEmpty(valid, "未解析到有效数据（仓库编码、商品编号、数量、未税单价必填）");
        resp.setValidRows(valid.size());
        resp.setDuplicateRows(0);
        resp.setImportKey(PsiImportHelper.cacheRows(valid));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PsiImportResp importData(PsiImportReq req) {
        List<InitStockImportRowReq> rows = PsiImportHelper.loadRows(req.getImportKey(), InitStockImportRowReq.class);
        Long tenantId = TenantFillHelper.currentTenantId();
        // 按 仓库+期初日期 分组生成草稿单
        Map<String, List<InitStockImportRowReq>> groups = new LinkedHashMap<>();
        for (InitStockImportRowReq row : rows) {
            String dateKey = StrUtil.blankToDefault(StrUtil.trim(row.getInitDate()), LocalDate.now().toString());
            String key = row.getWarehouseCode().trim() + "|" + dateKey;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        int insert = 0;
        for (Map.Entry<String, List<InitStockImportRowReq>> e : groups.entrySet()) {
            List<InitStockImportRowReq> group = e.getValue();
            InitStockImportRowReq first = group.get(0);
            WarehouseDO wh = resolveWarehouse(tenantId, first.getWarehouseCode());
            LocalDateTime initDate = StrUtil.isBlank(first.getInitDate())
                ? LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
                : parseInitDate(first.getInitDate());
            InitStockReq createReq = new InitStockReq();
            createReq.setWarehouseId(wh.getId());
            createReq.setInitDate(initDate);
            createReq.setRemark(StrUtil.blankToDefault(StrUtil.trim(first.getRemark()), "Excel瀵煎叆"));
            List<InitStockReq.Item> items = new ArrayList<>();
            for (InitStockImportRowReq row : group) {
                GoodsDO goods = resolveGoods(tenantId, row.getGoodsCode());
                InitStockReq.Item item = new InitStockReq.Item();
                item.setGoodsId(goods.getId());
                item.setQuantity(parseDecimal(row.getQuantity(), "数量"));
                item.setUntaxPrice(parseDecimal(row.getUntaxPrice(), "未税单价"));
                items.add(item);
            }
            createReq.setItems(items);
            this.create(createReq);
            insert++;
        }
        PsiImportHelper.clear(req.getImportKey());
        return new PsiImportResp(rows.size(), insert, 0, 0);
    }

    private WarehouseDO resolveWarehouse(Long tenantId, String code) {
        WarehouseDO wh = warehouseMapper.selectOne(Wrappers.lambdaQuery(WarehouseDO.class)
            .eq(WarehouseDO::getTenantId, tenantId)
            .eq(WarehouseDO::getWarehouseCode, code.trim())
            .last("LIMIT 1"));
        if (wh == null) {
            throw new BusinessException("仓库编码不存在：" + code);
        }
        return wh;
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

    private BigDecimal parseDecimal(String raw, String label) {
        String v = raw.trim().replace(",", "");
        if (!NumberUtil.isNumber(v)) {
            throw new BusinessException(label + "格式不正确：" + raw);
        }
        BigDecimal n = new BigDecimal(v);
        if (n.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(label + "必须大于 0：" + raw);
        }
        return n;
    }

    private LocalDateTime parseInitDate(String raw) {
        String v = raw.trim().replace('/', '-');
        try {
            if (v.length() >= 10) {
                return LocalDateTime.of(LocalDate.parse(v.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE), LocalTime.MIN);
            }
            return LocalDateTime.of(LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE), LocalTime.MIN);
        } catch (Exception e) {
            throw new BusinessException("期初日期格式不正确（yyyy-MM-dd）：" + raw);
        }
    }

    private void saveItems(Long tenantId, Long initId, List<InitStockReq.Item> lines) {
        int sort = 0;
        for (InitStockReq.Item line : lines) {
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("期初数量必须大于 0");
            }
            BigDecimal amount = line.getQuantity()
                .multiply(line.getUntaxPrice())
                .setScale(2, RoundingMode.HALF_UP);
            InitStockItemDO item = new InitStockItemDO();
            item.setTenantId(tenantId);
            item.setInitId(initId);
            item.setGoodsId(line.getGoodsId());
            item.setQuantity(line.getQuantity());
            item.setUntaxPrice(line.getUntaxPrice());
            item.setUntaxAmount(amount);
            item.setSort(sort++);
            initStockItemMapper.insert(item);
        }
    }

    private InitStockDO require(Long id) {
        InitStockDO entity = initStockMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("期初单不存在");
        }
        return entity;
    }

    private void fillWarehouseNames(List<InitStockResp> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> ids = list.stream().map(InitStockResp::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = new HashMap<>();
        for (WarehouseDO w : warehouseMapper.selectByIds(ids)) {
            names.put(w.getId(), w.getWarehouseName());
        }
        for (InitStockResp row : list) {
            if (row.getWarehouseId() != null) {
                row.setWarehouseName(names.get(row.getWarehouseId()));
            }
        }
    }

    private void assertDraft(InitStockDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿可操作");
        }
    }
}
