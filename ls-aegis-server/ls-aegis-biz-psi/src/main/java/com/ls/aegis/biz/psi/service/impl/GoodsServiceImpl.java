package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.enums.PsiImportPolicyEnum;
import com.ls.aegis.biz.psi.mapper.GoodsCategoryMapper;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.model.entity.GoodsCategoryDO;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.query.GoodsQuery;
import com.ls.aegis.biz.psi.model.req.GoodsImportRowReq;
import com.ls.aegis.biz.psi.model.req.GoodsReq;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.resp.GoodsDetailResp;
import com.ls.aegis.biz.psi.model.resp.GoodsResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.service.GoodsService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.PsiImportHelper;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.mybatis.base.service.BaseServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.CheckUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends BaseServiceImpl<GoodsMapper, GoodsDO, GoodsResp, GoodsDetailResp, GoodsQuery, GoodsReq> implements GoodsService {

    private final DocNoGenerator docNoGenerator;
    private final com.ls.aegis.biz.psi.support.MasterDataReferenceGuard referenceGuard;
    private final GoodsCategoryMapper goodsCategoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) { super.delete(ids); }

    @Override
    public void beforeDelete(List<Long> ids) { referenceGuard.requireDeletable("GOODS", ids); }

    @Override
    public void beforeCreate(GoodsReq req) {
        req.setTenantId(TenantFillHelper.currentTenantId());
        req.setDeptId(TenantFillHelper.currentDeptId());
        requireSecondLevelCategory(req.getCategoryId());
        if (StrUtil.isBlank(req.getGoodsCode())) {
            req.setGoodsCode(buildGoodsCode(req.getTenantId(), req.getCategoryId()));
        }
    }

    private String buildGoodsCode(Long tenantId, Long categoryId) {
        GoodsCategoryDO category = goodsCategoryMapper.selectById(categoryId);
        if (category == null || StrUtil.isBlank(category.getCode())) {
            throw new BusinessException("商品分类不存在或缺少编码");
        }
        return docNoGenerator.nextGoodsCode(tenantId, category.getCode());
    }

    @Override
    public void beforeUpdate(GoodsReq req, Long id) {
        referenceGuard.requireEditable("GOODS", id);
        // Normal edits cannot move persisted ownership, including administrator edits.
        req.setDeptId(null);
        req.setTenantId(null);
        if (req.getCategoryId() != null) {
            requireSecondLevelCategory(req.getCategoryId());
        }
    }

    /** 商品只能挂二级分类，保证编码=二级分类码+序号 */
    private void requireSecondLevelCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException("请选择二级商品分类");
        }
        GoodsCategoryDO category = goodsCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }
        if (category.getParentId() == null || category.getParentId() == 0L) {
            throw new BusinessException("商品不允许选择一级分类，请选择二级分类");
        }
        if (StrUtil.isBlank(category.getCode())) {
            throw new BusinessException("商品分类缺少编码");
        }
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        PsiImportHelper.downloadTemplate(response, GoodsImportRowReq.class, "商品导入模板.xlsx");
    }

    @Override
    public PsiImportParseResp parseImport(MultipartFile file) {
        CheckUtils.throwIf(file == null || file.isEmpty(), "文件不能为空");
        List<GoodsImportRowReq> rows;
        try {
            rows = PsiImportHelper.readExcel(file.getInputStream(), GoodsImportRowReq.class);
        } catch (Exception e) {
            throw new BusinessException("数据文件解析异常，请检查模板格式");
        }
        PsiImportParseResp resp = new PsiImportParseResp();
        resp.setTotalRows(rows.size());
        List<GoodsImportRowReq> valid = rows.stream()
            .filter(r -> StrUtil.isNotBlank(r.getGoodsName()) && StrUtil.isNotBlank(r.getCategoryCode()))
            .toList();
        CheckUtils.throwIfEmpty(valid, "未解析到有效数据（商品名称、分类编码必填）");
        Long tenantId = TenantFillHelper.currentTenantId();
        Set<String> nameKeys = new HashSet<>();
        for (GoodsImportRowReq r : valid) {
            resolveCategory(tenantId, r.getCategoryCode());
            CheckUtils.throwIfBlank(r.getUnit(), "计量单位不能为空：" + r.getGoodsName());
            CheckUtils.throwIfBlank(r.getDefaultTaxRate(), "默认税率不能为空：" + r.getGoodsName());
            String key = r.getGoodsName().trim() + "|" + r.getCategoryCode().trim();
            CheckUtils.throwIf(!nameKeys.add(key), "表格内同分类下商品名称重复：" + r.getGoodsName());
        }
        int dup = 0;
        for (GoodsImportRowReq r : valid) {
            if (findExist(tenantId, r) != null) {
                dup++;
            }
        }
        resp.setValidRows(valid.size());
        resp.setDuplicateRows(dup);
        resp.setImportKey(PsiImportHelper.cacheRows(valid));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PsiImportResp importData(PsiImportReq req) {
        List<GoodsImportRowReq> rows = PsiImportHelper.loadRows(req.getImportKey(), GoodsImportRowReq.class);
        PsiImportPolicyEnum policy = req.getDuplicatePolicy() == null ? PsiImportPolicyEnum.SKIP : req.getDuplicatePolicy();
        Long tenantId = TenantFillHelper.currentTenantId();
        int insert = 0;
        int update = 0;
        int skip = 0;
        for (GoodsImportRowReq row : rows) {
            GoodsDO exist = findExist(tenantId, row);
            if (exist != null) {
                if (policy == PsiImportPolicyEnum.EXIT) {
                    throw new BusinessException("商品已存在，已按策略停止导入：" + row.getGoodsName());
                }
                if (policy == PsiImportPolicyEnum.SKIP) {
                    skip++;
                    continue;
                }
                GoodsReq updateReq = toReq(tenantId, row, req.getDefaultStatus());
                updateReq.setGoodsCode(null);
                this.update(updateReq, exist.getId());
                update++;
            } else {
                this.create(toReq(tenantId, row, req.getDefaultStatus()));
                insert++;
            }
        }
        PsiImportHelper.clear(req.getImportKey());
        return new PsiImportResp(rows.size(), insert, update, skip);
    }

    private GoodsDO findExist(Long tenantId, GoodsImportRowReq row) {
        if (StrUtil.isNotBlank(row.getGoodsCode())) {
            GoodsDO byCode = baseMapper.selectOne(Wrappers.lambdaQuery(GoodsDO.class)
                .eq(GoodsDO::getTenantId, tenantId)
                .eq(GoodsDO::getGoodsCode, row.getGoodsCode().trim())
                .last("LIMIT 1"));
            if (byCode != null) {
                return byCode;
            }
        }
        GoodsCategoryDO cat = resolveCategory(tenantId, row.getCategoryCode());
        return baseMapper.selectOne(Wrappers.lambdaQuery(GoodsDO.class)
            .eq(GoodsDO::getTenantId, tenantId)
            .eq(GoodsDO::getCategoryId, cat.getId())
            .eq(GoodsDO::getGoodsName, row.getGoodsName().trim())
            .last("LIMIT 1"));
    }

    private GoodsCategoryDO resolveCategory(Long tenantId, String categoryCode) {
        CheckUtils.throwIfBlank(categoryCode, "分类编码不能为空");
        GoodsCategoryDO cat = goodsCategoryMapper.selectOne(Wrappers.lambdaQuery(GoodsCategoryDO.class)
            .eq(GoodsCategoryDO::getTenantId, tenantId)
            .eq(GoodsCategoryDO::getCode, categoryCode.trim())
            .last("LIMIT 1"));
        if (cat == null) {
            throw new BusinessException("分类编码不存在：" + categoryCode);
        }
        if (cat.getParentId() == null || cat.getParentId() == 0L) {
            throw new BusinessException("商品只能挂二级分类，请填写二级分类编码：" + categoryCode);
        }
        return cat;
    }

    private GoodsReq toReq(Long tenantId, GoodsImportRowReq row, Integer defaultStatus) {
        GoodsCategoryDO cat = resolveCategory(tenantId, row.getCategoryCode());
        GoodsReq req = new GoodsReq();
        req.setGoodsCode(StrUtil.blankToDefault(StrUtil.trim(row.getGoodsCode()), null));
        req.setGoodsName(StrUtil.trim(row.getGoodsName()));
        req.setCategoryId(cat.getId());
        req.setSpec(StrUtil.trim(row.getSpec()));
        req.setUnit(StrUtil.trim(row.getUnit()));
        req.setDefaultTaxRate(new BigDecimal(row.getDefaultTaxRate().trim()));
        req.setBarcode(StrUtil.trim(row.getBarcode()));
        req.setPurchasePrice(parseDecimal(row.getPurchasePrice()));
        req.setSalePrice(parseDecimal(row.getSalePrice()));
        req.setStockLower(parseDecimal(row.getStockLower()));
        req.setStockUpper(parseDecimal(row.getStockUpper()));
        req.setStatus(PsiImportHelper.parseStatus(row.getStatus(), defaultStatus));
        req.setRemark(StrUtil.trim(row.getRemark()));
        return req;
    }

    private BigDecimal parseDecimal(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        if (!NumberUtil.isNumber(raw.trim())) {
            throw new BusinessException("数值格式不正确：" + raw);
        }
        return new BigDecimal(raw.trim());
    }

    @Override
    protected void fill(Object obj) {
        super.fill(obj);
        if (obj instanceof GoodsResp resp) {
            fillCategoryName(List.of(resp));
        } else if (obj instanceof GoodsDetailResp detail) {
            fillDetailCategoryName(List.of(detail));
        } else if (obj instanceof List<?> list && CollUtil.isNotEmpty(list)) {
            if (list.get(0) instanceof GoodsResp) {
                @SuppressWarnings("unchecked")
                List<GoodsResp> rows = (List<GoodsResp>) list;
                fillCategoryName(rows);
            } else if (list.get(0) instanceof GoodsDetailResp) {
                @SuppressWarnings("unchecked")
                List<GoodsDetailResp> rows = (List<GoodsDetailResp>) list;
                fillDetailCategoryName(rows);
            }
        }
    }

    private void fillCategoryName(List<GoodsResp> rows) {
        List<Long> ids = rows.stream().map(GoodsResp::getCategoryId).filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        Map<Long, String> nameMap = goodsCategoryMapper.selectList(Wrappers.lambdaQuery(GoodsCategoryDO.class)
            .in(GoodsCategoryDO::getId, ids))
            .stream()
            .collect(Collectors.toMap(GoodsCategoryDO::getId, GoodsCategoryDO::getName, (a, b) -> a));
        for (GoodsResp row : rows) {
            if (row.getCategoryId() != null) {
                row.setCategoryName(nameMap.get(row.getCategoryId()));
            }
        }
    }

    private void fillDetailCategoryName(List<GoodsDetailResp> rows) {
        List<Long> ids = rows.stream().map(GoodsDetailResp::getCategoryId).filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        Map<Long, String> nameMap = goodsCategoryMapper.selectList(Wrappers.lambdaQuery(GoodsCategoryDO.class)
            .in(GoodsCategoryDO::getId, ids))
            .stream()
            .collect(Collectors.toMap(GoodsCategoryDO::getId, GoodsCategoryDO::getName, (a, b) -> a));
        for (GoodsDetailResp row : rows) {
            if (row.getCategoryId() != null) {
                row.setCategoryName(nameMap.get(row.getCategoryId()));
            }
        }
    }
}
