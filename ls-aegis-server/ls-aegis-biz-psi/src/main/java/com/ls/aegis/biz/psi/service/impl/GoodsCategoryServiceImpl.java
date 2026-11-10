package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.PsiImportPolicyEnum;
import com.ls.aegis.biz.psi.mapper.GoodsCategoryMapper;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.model.entity.GoodsCategoryDO;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.query.GoodsCategoryQuery;
import com.ls.aegis.biz.psi.model.req.GoodsCategoryImportRowReq;
import com.ls.aegis.biz.psi.model.req.GoodsCategoryReq;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.resp.GoodsCategoryExportResp;
import com.ls.aegis.biz.psi.model.resp.GoodsCategoryResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.service.GoodsCategoryService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.PsiImportHelper;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.mybatis.base.service.BaseServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.CheckUtils;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品分类实现（仅一级 / 二级）
 */
@Service
@RequiredArgsConstructor
public class GoodsCategoryServiceImpl extends BaseServiceImpl<GoodsCategoryMapper, GoodsCategoryDO, GoodsCategoryResp, GoodsCategoryResp, GoodsCategoryQuery, GoodsCategoryReq> implements GoodsCategoryService {

    private final GoodsMapper goodsMapper;
    private final DocNoGenerator docNoGenerator;

    @Override
    public void beforeCreate(GoodsCategoryReq req) {
        Long tenantId = TenantFillHelper.currentTenantId();
        req.setTenantId(tenantId);
        req.setDeptId(TenantFillHelper.currentDeptId());
        if (req.getParentId() == null) {
            req.setParentId(0L);
        }
        if (req.getSort() == null) {
            req.setSort(999);
        }
        checkParent(req.getParentId(), null);
        // 编码一律按层级自动生成（一级 10/20/30，二级 1001/2001…）
        req.setCode(buildCategoryCode(tenantId, req.getParentId()));
        checkCodeUnique(tenantId, req.getCode(), null);
    }

    @Override
    public void beforeUpdate(GoodsCategoryReq req, Long id) {
        // 编码创建后不允许改，避免商品号规则漂移
        req.setCode(null);
        req.setTenantId(null);
        req.setDeptId(null);
        if (req.getParentId() == null) {
            req.setParentId(0L);
        }
        CheckUtils.throwIf(ObjectUtil.equal(id, req.getParentId()), "上级分类不能选择自己");
        checkParent(req.getParentId(), id);
        GoodsCategoryDO current = baseMapper.selectById(id);
        if (current != null && !ObjectUtil.equal(current.getParentId(), req.getParentId())) {
            throw new BusinessException("已生成编码的分类不允许调整上级，请新建分类");
        }
    }

    @Override
    public void beforeDelete(List<Long> ids) {
        for (Long id : ids) {
            boolean hasChild = baseMapper.lambdaQuery()
                .eq(GoodsCategoryDO::getParentId, id)
                .exists();
            CheckUtils.throwIf(hasChild, "存在下级分类，不允许删除");
            boolean used = goodsMapper.selectCount(Wrappers.lambdaQuery(GoodsDO.class)
                .eq(GoodsDO::getCategoryId, id)) > 0;
            CheckUtils.throwIf(used, "分类下仍有商品，不允许删除");
        }
    }

    @Override
    protected void fill(Object obj) {
        super.fill(obj);
        fillLevel(obj);
    }

    private void fillLevel(Object obj) {
        if (obj instanceof GoodsCategoryResp resp) {
            applyLevel(resp);
            if (CollUtil.isNotEmpty(resp.getChildren())) {
                for (GoodsCategoryResp child : resp.getChildren()) {
                    fillLevel(child);
                }
            }
        } else if (obj instanceof List<?> list) {
            for (Object item : list) {
                fillLevel(item);
            }
        }
    }

    private void applyLevel(GoodsCategoryResp resp) {
        boolean root = resp.getParentId() == null || resp.getParentId() == 0L;
        resp.setLevel(root ? 1 : 2);
        resp.setLevelName(root ? "一级" : "二级");
    }

    private String buildCategoryCode(Long tenantId, Long parentId) {
        String parentCode = null;
        if (parentId != null && parentId != 0L) {
            GoodsCategoryDO parent = baseMapper.selectById(parentId);
            if (parent == null || StrUtil.isBlank(parent.getCode())) {
                throw new BusinessException("上级分类编码不存在，请先完善上级分类编码");
            }
            parentCode = parent.getCode();
        }
        return docNoGenerator.nextCategoryCode(tenantId, parentId, parentCode);
    }

    private void checkCodeUnique(Long tenantId, String code, Long excludeId) {
        boolean exists = baseMapper.lambdaQuery()
            .eq(GoodsCategoryDO::getTenantId, tenantId)
            .eq(GoodsCategoryDO::getCode, code)
            .ne(excludeId != null, GoodsCategoryDO::getId, excludeId)
            .exists();
        CheckUtils.throwIf(exists, "分类编码 [{}] 已存在", code);
    }

    private void checkParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        GoodsCategoryDO parent = baseMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("上级分类不存在");
        }
        // 仅两级：上级必须是一级（parentId=0），不可挂在二级下再建三级
        if (parent.getParentId() != null && parent.getParentId() != 0L) {
            throw new BusinessException("仅支持一级、二级分类，请选择一级分类作为上级");
        }
        if (selfId != null) {
            Long cursor = parent.getParentId();
            int guard = 0;
            while (cursor != null && cursor != 0L && guard++ < PsiConstants.CATEGORY_MAX_LEVEL + 2) {
                if (ObjectUtil.equal(cursor, selfId)) {
                    throw new BusinessException("上级分类不能选择自己的下级");
                }
                GoodsCategoryDO p = baseMapper.selectById(cursor);
                if (p == null) {
                    break;
                }
                cursor = p.getParentId();
            }
        }
    }

    @Override
    public void export(GoodsCategoryQuery query, HttpServletResponse response) {
        Long tenantId = TenantFillHelper.currentTenantId();
        List<GoodsCategoryDO> all = baseMapper.selectList(Wrappers.lambdaQuery(GoodsCategoryDO.class)
            .eq(GoodsCategoryDO::getTenantId, tenantId)
            .like(StrUtil.isNotBlank(query.getName()), GoodsCategoryDO::getName, StrUtil.trim(query.getName()))
            .orderByAsc(GoodsCategoryDO::getCode));
        Map<Long, String> codeById = new HashMap<>();
        for (GoodsCategoryDO row : all) {
            codeById.put(row.getId(), row.getCode());
        }
        List<GoodsCategoryExportResp> rows = new ArrayList<>();
        for (GoodsCategoryDO row : all) {
            GoodsCategoryExportResp item = new GoodsCategoryExportResp();
            item.setCode(row.getCode());
            item.setName(row.getName());
            boolean root = row.getParentId() == null || row.getParentId() == 0L;
            item.setParentCode(root ? "" : codeById.getOrDefault(row.getParentId(), ""));
            item.setLevelName(root ? "一级" : "二级");
            item.setSort(row.getSort());
            item.setStatus(row.getStatus());
            rows.add(item);
        }
        ExcelExportHelper.export(rows, GoodsCategoryExportResp.class, "商品分类", response);
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        PsiImportHelper.downloadTemplate(response, GoodsCategoryImportRowReq.class, "商品分类导入模板.xlsx");
    }

    @Override
    public PsiImportParseResp parseImport(MultipartFile file) {
        CheckUtils.throwIf(file == null || file.isEmpty(), "文件不能为空");
        List<GoodsCategoryImportRowReq> rows;
        try {
            rows = PsiImportHelper.readExcel(file.getInputStream(), GoodsCategoryImportRowReq.class);
        } catch (Exception e) {
            throw new BusinessException("数据文件解析异常，请检查模板格式");
        }
        PsiImportParseResp resp = new PsiImportParseResp();
        resp.setTotalRows(rows.size());
        List<GoodsCategoryImportRowReq> valid = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        Long tenantId = TenantFillHelper.currentTenantId();
        for (GoodsCategoryImportRowReq row : rows) {
            if (StrUtil.isBlank(row.getName())) {
                continue;
            }
            String name = row.getName().trim();
            String parentCode = StrUtil.trim(row.getParentCode());
            String key = StrUtil.blankToDefault(parentCode, "") + "|" + name;
            CheckUtils.throwIf(!keys.add(key), "表格内同上级下分类名称重复：" + name);
            if (StrUtil.isNotBlank(parentCode)) {
                GoodsCategoryDO parent = findByCode(tenantId, parentCode);
                // 允许父级在本次导入一级中尚未落库：仅校验格式；真正 import 再解析
                if (parent != null && parent.getParentId() != null && parent.getParentId() != 0L) {
                    throw new BusinessException("上级必须是一级分类：" + parentCode);
                }
            }
            valid.add(row);
        }
        CheckUtils.throwIfEmpty(valid, "未解析到有效数据（分类名称必填）");
        int dup = 0;
        for (GoodsCategoryImportRowReq row : valid) {
            if (findExistByName(tenantId, row) != null) {
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
        List<GoodsCategoryImportRowReq> rows = PsiImportHelper.loadRows(req.getImportKey(), GoodsCategoryImportRowReq.class);
        PsiImportPolicyEnum policy = req.getDuplicatePolicy() == null ? PsiImportPolicyEnum.SKIP : req.getDuplicatePolicy();
        Long tenantId = TenantFillHelper.currentTenantId();
        // 先一级后二级
        List<GoodsCategoryImportRowReq> level1 = rows.stream().filter(r -> StrUtil.isBlank(r.getParentCode())).toList();
        List<GoodsCategoryImportRowReq> level2 = rows.stream().filter(r -> StrUtil.isNotBlank(r.getParentCode())).toList();
        int insert = 0;
        int update = 0;
        int skip = 0;
        for (GoodsCategoryImportRowReq row : level1) {
            int[] c = upsertCategory(tenantId, row, 0L, policy, req.getDefaultStatus());
            insert += c[0];
            update += c[1];
            skip += c[2];
        }
        for (GoodsCategoryImportRowReq row : level2) {
            GoodsCategoryDO parent = findByCode(tenantId, row.getParentCode().trim());
            if (parent == null) {
                throw new BusinessException("上级分类编码不存在：" + row.getParentCode());
            }
            if (parent.getParentId() != null && parent.getParentId() != 0L) {
                throw new BusinessException("上级必须是一级分类：" + row.getParentCode());
            }
            int[] c = upsertCategory(tenantId, row, parent.getId(), policy, req.getDefaultStatus());
            insert += c[0];
            update += c[1];
            skip += c[2];
        }
        PsiImportHelper.clear(req.getImportKey());
        return new PsiImportResp(rows.size(), insert, update, skip);
    }

    private int[] upsertCategory(Long tenantId, GoodsCategoryImportRowReq row, Long parentId,
                                 PsiImportPolicyEnum policy, Integer defaultStatus) {
        GoodsCategoryDO exist = findExistByName(tenantId, row);
        if (exist != null) {
            if (policy == PsiImportPolicyEnum.EXIT) {
                throw new BusinessException("分类已存在，已按策略停止导入：" + row.getName());
            }
            if (policy == PsiImportPolicyEnum.SKIP) {
                return new int[]{0, 0, 1};
            }
            GoodsCategoryReq updateReq = toReq(row, parentId, defaultStatus);
            updateReq.setCode(null);
            this.update(updateReq, exist.getId());
            return new int[]{0, 1, 0};
        }
        this.create(toReq(row, parentId, defaultStatus));
        return new int[]{1, 0, 0};
    }

    private GoodsCategoryDO findExistByName(Long tenantId, GoodsCategoryImportRowReq row) {
        Long parentId = 0L;
        if (StrUtil.isNotBlank(row.getParentCode())) {
            GoodsCategoryDO parent = findByCode(tenantId, row.getParentCode().trim());
            if (parent == null) {
                return null;
            }
            parentId = parent.getId();
        }
        return baseMapper.selectOne(Wrappers.lambdaQuery(GoodsCategoryDO.class)
            .eq(GoodsCategoryDO::getTenantId, tenantId)
            .eq(GoodsCategoryDO::getParentId, parentId)
            .eq(GoodsCategoryDO::getName, row.getName().trim())
            .last("LIMIT 1"));
    }

    private GoodsCategoryDO findByCode(Long tenantId, String code) {
        return baseMapper.selectOne(Wrappers.lambdaQuery(GoodsCategoryDO.class)
            .eq(GoodsCategoryDO::getTenantId, tenantId)
            .eq(GoodsCategoryDO::getCode, code.trim())
            .last("LIMIT 1"));
    }

    private GoodsCategoryReq toReq(GoodsCategoryImportRowReq row, Long parentId, Integer defaultStatus) {
        GoodsCategoryReq req = new GoodsCategoryReq();
        req.setParentId(parentId == null ? 0L : parentId);
        req.setName(StrUtil.trim(row.getName()));
        if (StrUtil.isNotBlank(row.getSort()) && StrUtil.isNumeric(row.getSort().trim())) {
            req.setSort(Integer.parseInt(row.getSort().trim()));
        } else {
            req.setSort(999);
        }
        DisEnableStatusEnum status = PsiImportHelper.parseStatus(row.getStatus(), defaultStatus);
        req.setStatus(status);
        return req;
    }
}
