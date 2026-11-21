package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InitTypeEnum;
import com.ls.aegis.biz.psi.enums.OpenItemStatusEnum;
import com.ls.aegis.biz.psi.mapper.InitPayableItemMapper;
import com.ls.aegis.biz.psi.mapper.InitPayableMapper;
import com.ls.aegis.biz.psi.mapper.InitPrepayItemMapper;
import com.ls.aegis.biz.psi.mapper.InitPrepayMapper;
import com.ls.aegis.biz.psi.mapper.InitReceivableItemMapper;
import com.ls.aegis.biz.psi.mapper.InitReceivableMapper;
import com.ls.aegis.biz.psi.mapper.InitStockItemMapper;
import com.ls.aegis.biz.psi.mapper.InitStockMapper;
import com.ls.aegis.biz.psi.mapper.PayableMapper;
import com.ls.aegis.biz.psi.mapper.PsiPaymentMapper;
import com.ls.aegis.biz.psi.mapper.PsiReceivableMapper;
import com.ls.aegis.biz.psi.model.entity.InitPayableDO;
import com.ls.aegis.biz.psi.model.entity.InitPayableItemDO;
import com.ls.aegis.biz.psi.model.entity.InitPrepayDO;
import com.ls.aegis.biz.psi.model.entity.InitPrepayItemDO;
import com.ls.aegis.biz.psi.model.entity.InitReceivableDO;
import com.ls.aegis.biz.psi.model.entity.InitReceivableItemDO;
import com.ls.aegis.biz.psi.model.entity.InitStockDO;
import com.ls.aegis.biz.psi.model.entity.InitStockItemDO;
import com.ls.aegis.biz.psi.model.entity.PayableDO;
import com.ls.aegis.biz.psi.model.entity.PaymentDO;
import com.ls.aegis.biz.psi.model.entity.ReceivableDO;
import com.ls.aegis.biz.psi.model.query.InitDocQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InitDocReq;
import com.ls.aegis.biz.psi.model.resp.InitDocResp;
import com.ls.aegis.biz.psi.service.InitDocService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.FifoStockHelper;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InitDocServiceImpl implements InitDocService {

    private final InitStockMapper initStockMapper;
    private final InitStockItemMapper initStockItemMapper;
    private final InitReceivableMapper initReceivableMapper;
    private final InitReceivableItemMapper initReceivableItemMapper;
    private final InitPayableMapper initPayableMapper;
    private final InitPayableItemMapper initPayableItemMapper;
    private final InitPrepayMapper initPrepayMapper;
    private final InitPrepayItemMapper initPrepayItemMapper;
    private final PsiReceivableMapper receivableMapper;
    private final PayableMapper payableMapper;
    private final PsiPaymentMapper paymentMapper;
    private final DocNoGenerator docNoGenerator;
    private final FifoStockHelper fifoStockHelper;
    private final IdempotentHelper idempotentHelper;

    @Override
    public PageResp<InitDocResp> page(InitDocQuery query, PageQuery pageQuery) {
        InitTypeEnum type = requireType(query.getType());
        return switch (type) {
            case STOCK -> pageStock(query, pageQuery);
            case AR -> pageAr(query, pageQuery);
            case AP -> pageAp(query, pageQuery);
            case PREPAY -> pagePrepay(query, pageQuery);
        };
    }

    @Override
    public InitDocResp get(String type, Long id) {
        return switch (parseType(type)) {
            case STOCK -> getStock(id);
            case AR -> getAr(id);
            case AP -> getAp(id);
            case PREPAY -> getPrepay(id);
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(InitDocReq req) {
        InitTypeEnum type = requireType(req.getType());
        Long tenantId = TenantFillHelper.currentTenantId();
        return switch (type) {
            case STOCK -> createStock(tenantId, req);
            case AR -> createAr(tenantId, req);
            case AP -> createAp(tenantId, req);
            case PREPAY -> createPrepay(tenantId, req);
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(InitDocReq req, Long id) {
        InitTypeEnum type = requireType(req.getType());
        switch (type) {
            case STOCK -> updateStock(id, req);
            case AR -> updateAr(id, req);
            case AP -> updateAp(id, req);
            case PREPAY -> updatePrepay(id, req);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String type, Long id) {
        switch (parseType(type)) {
            case STOCK -> {
                InitStockDO e = requireStock(id);
                assertDraft(e.getDocStatus());
                initStockItemMapper.delete(Wrappers.lambdaQuery(InitStockItemDO.class).eq(InitStockItemDO::getInitId, id));
                initStockMapper.deleteById(id);
            }
            case AR -> {
                InitReceivableDO e = requireAr(id);
                assertDraft(e.getDocStatus());
                initReceivableItemMapper.delete(Wrappers.lambdaQuery(InitReceivableItemDO.class).eq(InitReceivableItemDO::getInitId, id));
                initReceivableMapper.deleteById(id);
            }
            case AP -> {
                InitPayableDO e = requireAp(id);
                assertDraft(e.getDocStatus());
                initPayableItemMapper.delete(Wrappers.lambdaQuery(InitPayableItemDO.class).eq(InitPayableItemDO::getInitId, id));
                initPayableMapper.deleteById(id);
            }
            case PREPAY -> {
                InitPrepayDO e = requirePrepay(id);
                assertDraft(e.getDocStatus());
                initPrepayItemMapper.delete(Wrappers.lambdaQuery(InitPrepayItemDO.class).eq(InitPrepayItemDO::getInitId, id));
                initPrepayMapper.deleteById(id);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirm(String type, Long id, DocConfirmReq req) {
        InitTypeEnum t = parseType(type);
        return switch (t) {
            case STOCK -> {
                InitStockDO locked = initStockMapper.selectByIdForUpdate(id);
                if (locked == null) {
                    throw new BusinessException("期初库存单不存在");
                }
                Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
                yield idempotentHelper.execute(tenantId, "init-stock-confirm-" + id, req.getIdempotentKey(), () -> confirmStock(locked, req.getIdempotentKey()));
            }
            case AR -> {
                InitReceivableDO locked = initReceivableMapper.selectByIdForUpdate(id);
                if (locked == null) {
                    throw new BusinessException("应收期初单不存在");
                }
                Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
                yield idempotentHelper.execute(tenantId, "init-ar-confirm-" + id, req.getIdempotentKey(), () -> confirmAr(locked, req.getIdempotentKey()));
            }
            case AP -> {
                InitPayableDO locked = initPayableMapper.selectByIdForUpdate(id);
                if (locked == null) {
                    throw new BusinessException("应付期初单不存在");
                }
                Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
                yield idempotentHelper.execute(tenantId, "init-ap-confirm-" + id, req.getIdempotentKey(), () -> confirmAp(locked, req.getIdempotentKey()));
            }
            case PREPAY -> {
                InitPrepayDO locked = initPrepayMapper.selectByIdForUpdate(id);
                if (locked == null) {
                    throw new BusinessException("预付期初单不存在");
                }
                Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
                yield idempotentHelper.execute(tenantId, "init-prepay-confirm-" + id, req.getIdempotentKey(), () -> confirmPrepay(locked, req.getIdempotentKey()));
            }
        };
    }

    private PageResp<InitDocResp> pageStock(InitDocQuery query, PageQuery pageQuery) {
        InitDocQuery q = new InitDocQuery();
        q.setDocNo(query.getDocNo());
        q.setDocStatus(query.getDocStatus());
        QueryWrapper<InitStockDO> wrapper = QueryWrapperHelper.build(q);
        wrapper.orderByDesc("id");
        Page<InitStockDO> page = initStockMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<InitDocResp> resp = PageResp.build(page, InitDocResp.class);
        if (resp.getList() != null) {
            resp.getList().forEach(r -> r.setType(InitTypeEnum.STOCK));
        }
        return resp;
    }

    private PageResp<InitDocResp> pageAr(InitDocQuery query, PageQuery pageQuery) {
        InitDocQuery q = new InitDocQuery();
        q.setDocNo(query.getDocNo());
        q.setDocStatus(query.getDocStatus());
        QueryWrapper<InitReceivableDO> wrapper = QueryWrapperHelper.build(q);
        wrapper.orderByDesc("id");
        Page<InitReceivableDO> page = initReceivableMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<InitDocResp> resp = PageResp.build(page, InitDocResp.class);
        if (resp.getList() != null) {
            resp.getList().forEach(r -> r.setType(InitTypeEnum.AR));
        }
        return resp;
    }

    private PageResp<InitDocResp> pageAp(InitDocQuery query, PageQuery pageQuery) {
        InitDocQuery q = new InitDocQuery();
        q.setDocNo(query.getDocNo());
        q.setDocStatus(query.getDocStatus());
        QueryWrapper<InitPayableDO> wrapper = QueryWrapperHelper.build(q);
        wrapper.orderByDesc("id");
        Page<InitPayableDO> page = initPayableMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<InitDocResp> resp = PageResp.build(page, InitDocResp.class);
        if (resp.getList() != null) {
            resp.getList().forEach(r -> r.setType(InitTypeEnum.AP));
        }
        return resp;
    }

    private PageResp<InitDocResp> pagePrepay(InitDocQuery query, PageQuery pageQuery) {
        InitDocQuery q = new InitDocQuery();
        q.setDocNo(query.getDocNo());
        q.setDocStatus(query.getDocStatus());
        QueryWrapper<InitPrepayDO> wrapper = QueryWrapperHelper.build(q);
        wrapper.orderByDesc("id");
        Page<InitPrepayDO> page = initPrepayMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<InitDocResp> resp = PageResp.build(page, InitDocResp.class);
        if (resp.getList() != null) {
            resp.getList().forEach(r -> r.setType(InitTypeEnum.PREPAY));
        }
        return resp;
    }

    private InitDocResp getStock(Long id) {
        InitStockDO entity = requireStock(id);
        InitDocResp resp = BeanUtil.copyProperties(entity, InitDocResp.class);
        resp.setType(InitTypeEnum.STOCK);
        List<InitStockItemDO> items = initStockItemMapper.selectList(Wrappers.lambdaQuery(InitStockItemDO.class)
            .eq(InitStockItemDO::getInitId, id).orderByAsc(InitStockItemDO::getSort));
        List<InitDocResp.Item> list = new ArrayList<>();
        for (InitStockItemDO it : items) {
            InitDocResp.Item row = new InitDocResp.Item();
            row.setId(it.getId());
            row.setGoodsId(it.getGoodsId());
            row.setQty(it.getQuantity());
            row.setUntaxPrice(it.getUntaxPrice());
            list.add(row);
        }
        resp.setItems(list);
        return resp;
    }

    private InitDocResp getAr(Long id) {
        InitReceivableDO entity = requireAr(id);
        InitDocResp resp = BeanUtil.copyProperties(entity, InitDocResp.class);
        resp.setType(InitTypeEnum.AR);
        resp.setItems(mapOpenItems(initReceivableItemMapper.selectList(Wrappers.lambdaQuery(InitReceivableItemDO.class)
            .eq(InitReceivableItemDO::getInitId, id).orderByAsc(InitReceivableItemDO::getSort))));
        return resp;
    }

    private InitDocResp getAp(Long id) {
        InitPayableDO entity = requireAp(id);
        InitDocResp resp = BeanUtil.copyProperties(entity, InitDocResp.class);
        resp.setType(InitTypeEnum.AP);
        resp.setItems(mapOpenItemsAp(initPayableItemMapper.selectList(Wrappers.lambdaQuery(InitPayableItemDO.class)
            .eq(InitPayableItemDO::getInitId, id).orderByAsc(InitPayableItemDO::getSort))));
        return resp;
    }

    private InitDocResp getPrepay(Long id) {
        InitPrepayDO entity = requirePrepay(id);
        InitDocResp resp = BeanUtil.copyProperties(entity, InitDocResp.class);
        resp.setType(InitTypeEnum.PREPAY);
        resp.setItems(mapOpenItemsPre(initPrepayItemMapper.selectList(Wrappers.lambdaQuery(InitPrepayItemDO.class)
            .eq(InitPrepayItemDO::getInitId, id).orderByAsc(InitPrepayItemDO::getSort))));
        return resp;
    }

    private List<InitDocResp.Item> mapOpenItems(List<InitReceivableItemDO> items) {
        List<InitDocResp.Item> list = new ArrayList<>();
        for (InitReceivableItemDO it : items) {
            InitDocResp.Item row = new InitDocResp.Item();
            row.setId(it.getId());
            row.setUnitId(it.getUnitId());
            row.setTaxInclusiveAmount(it.getTaxInclusiveAmount());
            list.add(row);
        }
        return list;
    }

    private List<InitDocResp.Item> mapOpenItemsAp(List<InitPayableItemDO> items) {
        List<InitDocResp.Item> list = new ArrayList<>();
        for (InitPayableItemDO it : items) {
            InitDocResp.Item row = new InitDocResp.Item();
            row.setId(it.getId());
            row.setUnitId(it.getUnitId());
            row.setTaxInclusiveAmount(it.getTaxInclusiveAmount());
            list.add(row);
        }
        return list;
    }

    private List<InitDocResp.Item> mapOpenItemsPre(List<InitPrepayItemDO> items) {
        List<InitDocResp.Item> list = new ArrayList<>();
        for (InitPrepayItemDO it : items) {
            InitDocResp.Item row = new InitDocResp.Item();
            row.setId(it.getId());
            row.setUnitId(it.getUnitId());
            row.setTaxInclusiveAmount(it.getTaxInclusiveAmount());
            list.add(row);
        }
        return list;
    }

    private Long createStock(Long tenantId, InitDocReq req) {
        if (req.getWarehouseId() == null) {
            throw new BusinessException("仓库不能为空");
        }
        InitStockDO entity = new InitStockDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_INIT_STOCK));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setWarehouseId(req.getWarehouseId());
        entity.setInitDate(req.getInitDate());
        entity.setRemark(req.getRemark());
        initStockMapper.insert(entity);
        int sort = 1;
        for (InitDocReq.Item line : req.getItems()) {
            if (line.getGoodsId() == null || line.getQty() == null || line.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("期初库存明细数量无效");
            }
            if (line.getUntaxPrice() == null) {
                throw new BusinessException("期初库存未税单价不能为空");
            }
            InitStockItemDO item = new InitStockItemDO();
            item.setTenantId(tenantId);
            item.setInitId(entity.getId());
            item.setGoodsId(line.getGoodsId());
            item.setQuantity(line.getQty());
            item.setUntaxPrice(line.getUntaxPrice());
            item.setUntaxAmount(line.getQty().multiply(line.getUntaxPrice()).setScale(2, RoundingMode.HALF_UP));
            item.setSort(sort++);
            initStockItemMapper.insert(item);
        }
        return entity.getId();
    }

    private Long createAr(Long tenantId, InitDocReq req) {
        InitReceivableDO entity = new InitReceivableDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_INIT_AR));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setInitDate(req.getInitDate());
        entity.setRemark(req.getRemark());
        initReceivableMapper.insert(entity);
        insertOpenItemsAr(tenantId, entity.getId(), req.getItems());
        return entity.getId();
    }

    private Long createAp(Long tenantId, InitDocReq req) {
        InitPayableDO entity = new InitPayableDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_INIT_AP));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setInitDate(req.getInitDate());
        entity.setRemark(req.getRemark());
        initPayableMapper.insert(entity);
        insertOpenItemsAp(tenantId, entity.getId(), req.getItems());
        return entity.getId();
    }

    private Long createPrepay(Long tenantId, InitDocReq req) {
        InitPrepayDO entity = new InitPrepayDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_INIT_PREPAY));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setInitDate(req.getInitDate());
        entity.setRemark(req.getRemark());
        initPrepayMapper.insert(entity);
        insertOpenItemsPre(tenantId, entity.getId(), req.getItems());
        return entity.getId();
    }

    private void updateStock(Long id, InitDocReq req) {
        InitStockDO entity = requireStock(id);
        assertDraft(entity.getDocStatus());
        if (req.getWarehouseId() == null) {
            throw new BusinessException("仓库不能为空");
        }
        entity.setWarehouseId(req.getWarehouseId());
        entity.setInitDate(req.getInitDate());
        entity.setRemark(req.getRemark());
        initStockMapper.updateById(entity);
        initStockItemMapper.delete(Wrappers.lambdaQuery(InitStockItemDO.class).eq(InitStockItemDO::getInitId, id));
        int sort = 1;
        for (InitDocReq.Item line : req.getItems()) {
            InitStockItemDO item = new InitStockItemDO();
            item.setTenantId(entity.getTenantId());
            item.setInitId(id);
            item.setGoodsId(line.getGoodsId());
            item.setQuantity(line.getQty());
            item.setUntaxPrice(line.getUntaxPrice());
            item.setUntaxAmount(line.getQty().multiply(line.getUntaxPrice()).setScale(2, RoundingMode.HALF_UP));
            item.setSort(sort++);
            initStockItemMapper.insert(item);
        }
    }

    private void updateAr(Long id, InitDocReq req) {
        InitReceivableDO entity = requireAr(id);
        assertDraft(entity.getDocStatus());
        entity.setInitDate(req.getInitDate());
        entity.setRemark(req.getRemark());
        initReceivableMapper.updateById(entity);
        initReceivableItemMapper.delete(Wrappers.lambdaQuery(InitReceivableItemDO.class).eq(InitReceivableItemDO::getInitId, id));
        insertOpenItemsAr(entity.getTenantId(), id, req.getItems());
    }

    private void updateAp(Long id, InitDocReq req) {
        InitPayableDO entity = requireAp(id);
        assertDraft(entity.getDocStatus());
        entity.setInitDate(req.getInitDate());
        entity.setRemark(req.getRemark());
        initPayableMapper.updateById(entity);
        initPayableItemMapper.delete(Wrappers.lambdaQuery(InitPayableItemDO.class).eq(InitPayableItemDO::getInitId, id));
        insertOpenItemsAp(entity.getTenantId(), id, req.getItems());
    }

    private void updatePrepay(Long id, InitDocReq req) {
        InitPrepayDO entity = requirePrepay(id);
        assertDraft(entity.getDocStatus());
        entity.setInitDate(req.getInitDate());
        entity.setRemark(req.getRemark());
        initPrepayMapper.updateById(entity);
        initPrepayItemMapper.delete(Wrappers.lambdaQuery(InitPrepayItemDO.class).eq(InitPrepayItemDO::getInitId, id));
        insertOpenItemsPre(entity.getTenantId(), id, req.getItems());
    }

    private void insertOpenItemsAr(Long tenantId, Long initId, List<InitDocReq.Item> lines) {
        int sort = 1;
        for (InitDocReq.Item line : lines) {
            validateOpenLine(line);
            InitReceivableItemDO item = new InitReceivableItemDO();
            item.setTenantId(tenantId);
            item.setInitId(initId);
            item.setUnitId(line.getUnitId());
            item.setTaxInclusiveAmount(line.getTaxInclusiveAmount());
            item.setSort(sort++);
            initReceivableItemMapper.insert(item);
        }
    }

    private void insertOpenItemsAp(Long tenantId, Long initId, List<InitDocReq.Item> lines) {
        int sort = 1;
        for (InitDocReq.Item line : lines) {
            validateOpenLine(line);
            InitPayableItemDO item = new InitPayableItemDO();
            item.setTenantId(tenantId);
            item.setInitId(initId);
            item.setUnitId(line.getUnitId());
            item.setTaxInclusiveAmount(line.getTaxInclusiveAmount());
            item.setSort(sort++);
            initPayableItemMapper.insert(item);
        }
    }

    private void insertOpenItemsPre(Long tenantId, Long initId, List<InitDocReq.Item> lines) {
        int sort = 1;
        for (InitDocReq.Item line : lines) {
            validateOpenLine(line);
            InitPrepayItemDO item = new InitPrepayItemDO();
            item.setTenantId(tenantId);
            item.setInitId(initId);
            item.setUnitId(line.getUnitId());
            item.setTaxInclusiveAmount(line.getTaxInclusiveAmount());
            item.setSort(sort++);
            initPrepayItemMapper.insert(item);
        }
    }

    private void validateOpenLine(InitDocReq.Item line) {
        if (line.getUnitId() == null || line.getTaxInclusiveAmount() == null
            || line.getTaxInclusiveAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("往来单位与金额不能为空且必须大于0");
        }
    }

    private String confirmStock(InitStockDO locked, String idempotentKey) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked.getDocStatus());
        assertCreator(locked.getCreateUser());
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        List<InitStockItemDO> items = initStockItemMapper.selectList(Wrappers.lambdaQuery(InitStockItemDO.class)
            .eq(InitStockItemDO::getInitId, locked.getId()));
        fifoStockHelper.lockAll(items.stream().map(item -> new com.ls.aegis.biz.psi.support.StockMutationLock.Key(
            tenantId, locked.getWarehouseId(), item.getGoodsId())).toList());
        for (InitStockItemDO item : items) {
            BigDecimal amt = item.getQuantity().multiply(item.getUntaxPrice()).setScale(2, RoundingMode.HALF_UP);
            fifoStockHelper.inbound(tenantId, locked.getWarehouseId(), item.getGoodsId(), item.getQuantity(),
                item.getUntaxPrice(), amt, locked.getInitDate(), BizSourceTypeEnum.INIT_STOCK, locked.getId(), locked.getDocNo());
        }
        markConfirmed(locked, idempotentKey);
        initStockMapper.updateById(locked);
        return locked.getDocNo();
    }

    private String confirmAr(InitReceivableDO locked, String idempotentKey) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked.getDocStatus());
        assertCreator(locked.getCreateUser());
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        List<InitReceivableItemDO> items = initReceivableItemMapper.selectList(Wrappers.lambdaQuery(InitReceivableItemDO.class)
            .eq(InitReceivableItemDO::getInitId, locked.getId()));
        for (InitReceivableItemDO item : items) {
            ReceivableDO open = new ReceivableDO();
            open.setTenantId(tenantId);
            open.setDeptId(TenantFillHelper.currentDeptId());
            open.setUnitId(item.getUnitId());
            open.setSourceType(BizSourceTypeEnum.INIT_AR);
            open.setSourceId(locked.getId());
            open.setSourceNo(locked.getDocNo());
            open.setTaxInclusiveAmount(item.getTaxInclusiveAmount());
            open.setWrittenOffAmount(BigDecimal.ZERO);
            open.setUnwrittenAmount(item.getTaxInclusiveAmount());
            open.setDocStatus(OpenItemStatusEnum.OPEN);
            receivableMapper.insert(open);
        }
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(idempotentKey);
        locked.setConfirmUser(UserContextHolder.getUserId());
        locked.setConfirmTime(LocalDateTime.now());
        initReceivableMapper.updateById(locked);
        return locked.getDocNo();
    }

    private String confirmAp(InitPayableDO locked, String idempotentKey) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked.getDocStatus());
        assertCreator(locked.getCreateUser());
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        List<InitPayableItemDO> items = initPayableItemMapper.selectList(Wrappers.lambdaQuery(InitPayableItemDO.class)
            .eq(InitPayableItemDO::getInitId, locked.getId()));
        for (InitPayableItemDO item : items) {
            PayableDO open = new PayableDO();
            open.setTenantId(tenantId);
            open.setDeptId(TenantFillHelper.currentDeptId());
            open.setUnitId(item.getUnitId());
            open.setSourceType(BizSourceTypeEnum.INIT_AP);
            open.setSourceId(locked.getId());
            open.setSourceNo(locked.getDocNo());
            open.setTaxInclusiveAmount(item.getTaxInclusiveAmount());
            open.setWrittenOffAmount(BigDecimal.ZERO);
            open.setUnwrittenAmount(item.getTaxInclusiveAmount());
            open.setDocStatus(OpenItemStatusEnum.OPEN);
            payableMapper.insert(open);
        }
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(idempotentKey);
        locked.setConfirmUser(UserContextHolder.getUserId());
        locked.setConfirmTime(LocalDateTime.now());
        initPayableMapper.updateById(locked);
        return locked.getDocNo();
    }

    private String confirmPrepay(InitPrepayDO locked, String idempotentKey) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked.getDocStatus());
        assertCreator(locked.getCreateUser());
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        List<InitPrepayItemDO> items = initPrepayItemMapper.selectList(Wrappers.lambdaQuery(InitPrepayItemDO.class)
            .eq(InitPrepayItemDO::getInitId, locked.getId()));
        for (InitPrepayItemDO item : items) {
            PaymentDO payment = new PaymentDO();
            payment.setTenantId(tenantId);
            payment.setDeptId(TenantFillHelper.currentDeptId());
            payment.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_PAYMENT));
            payment.setDocStatus(DocStatusEnum.CONFIRMED);
            payment.setUnitId(item.getUnitId());
            payment.setAccountId(0L);
            payment.setAmount(item.getTaxInclusiveAmount());
            payment.setUnallocatedAmount(item.getTaxInclusiveAmount());
            payment.setPayMethod("INIT_PREPAY");
            payment.setPaymentDate(locked.getInitDate());
            payment.setRemark("预付期初生成：" + locked.getDocNo());
            payment.setConfirmUser(UserContextHolder.getUserId());
            payment.setConfirmTime(LocalDateTime.now());
            paymentMapper.insert(payment);
        }
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(idempotentKey);
        locked.setConfirmUser(UserContextHolder.getUserId());
        locked.setConfirmTime(LocalDateTime.now());
        initPrepayMapper.updateById(locked);
        return locked.getDocNo();
    }

    private void markConfirmed(InitStockDO locked, String idempotentKey) {
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(idempotentKey);
        locked.setConfirmUser(UserContextHolder.getUserId());
        locked.setConfirmTime(LocalDateTime.now());
    }

    private InitStockDO requireStock(Long id) {
        InitStockDO e = initStockMapper.selectById(id);
        if (e == null) {
            throw new BusinessException("期初库存单不存在");
        }
        return e;
    }

    private InitReceivableDO requireAr(Long id) {
        InitReceivableDO e = initReceivableMapper.selectById(id);
        if (e == null) {
            throw new BusinessException("应收期初单不存在");
        }
        return e;
    }

    private InitPayableDO requireAp(Long id) {
        InitPayableDO e = initPayableMapper.selectById(id);
        if (e == null) {
            throw new BusinessException("应付期初单不存在");
        }
        return e;
    }

    private InitPrepayDO requirePrepay(Long id) {
        InitPrepayDO e = initPrepayMapper.selectById(id);
        if (e == null) {
            throw new BusinessException("预付期初单不存在");
        }
        return e;
    }

    private InitTypeEnum requireType(InitTypeEnum type) {
        if (type == null) {
            throw new BusinessException("期初类型不能为空");
        }
        return type;
    }

    private InitTypeEnum parseType(String type) {
        if (StrUtil.isBlank(type)) {
            throw new BusinessException("期初类型不能为空");
        }
        try {
            return InitTypeEnum.valueOf(type.trim().toUpperCase());
        } catch (Exception e) {
            throw new BusinessException("不支持的期初类型：" + type);
        }
    }

    private void assertDraft(DocStatusEnum status) {
        if (!DocStatusEnum.DRAFT.equals(status)) {
            throw new BusinessException("仅草稿状态允许此操作");
        }
    }

    private void assertCreator(Long createUser) {
        if (!ObjectUtil.equal(createUser, UserContextHolder.getUserId())) {
            throw new BusinessException("仅制单人可确认生效");
        }
    }
}
