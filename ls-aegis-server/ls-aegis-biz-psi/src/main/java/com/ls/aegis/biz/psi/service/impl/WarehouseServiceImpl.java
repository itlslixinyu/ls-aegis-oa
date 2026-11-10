package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.PsiImportPolicyEnum;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.query.WarehouseQuery;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.req.WarehouseImportRowReq;
import com.ls.aegis.biz.psi.model.req.WarehouseReq;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.model.resp.WarehouseDetailResp;
import com.ls.aegis.biz.psi.model.resp.WarehouseResp;
import com.ls.aegis.biz.psi.service.WarehouseService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.PsiImportHelper;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.mybatis.base.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.CheckUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service("psiWarehouseServiceImpl")
@RequiredArgsConstructor
public class WarehouseServiceImpl extends BaseServiceImpl<PsiWarehouseMapper, WarehouseDO, WarehouseResp, WarehouseDetailResp, WarehouseQuery, WarehouseReq> implements WarehouseService {

    private final DocNoGenerator docNoGenerator;
    private final com.ls.aegis.biz.psi.support.MasterDataReferenceGuard referenceGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) { super.delete(ids); }

    @Override
    protected QueryWrapper<WarehouseDO> buildQueryWrapper(WarehouseQuery query) {
        QueryWrapper<WarehouseDO> wrapper = super.buildQueryWrapper(query);
        applyManagerScope(wrapper);
        return wrapper;
    }

    @Override
    public WarehouseDetailResp get(Long id) {
        assertCanAccess(id);
        return super.get(id);
    }

    @Override
    public void beforeCreate(WarehouseReq req) {
        req.setTenantId(TenantFillHelper.currentTenantId());
        req.setDeptId(TenantFillHelper.currentDeptId());
        if (StrUtil.isBlank(req.getWarehouseCode())) {
            req.setWarehouseCode(docNoGenerator.nextCode(req.getTenantId(), PsiConstants.DOC_PREFIX_WAREHOUSE));
        }
        enforceManagerAssignment(req);
        syncManagerName(req);
    }

    @Override
    public void beforeUpdate(WarehouseReq req, Long id) {
        referenceGuard.requireEditable("WAREHOUSE", id);
        // Normal edits cannot move persisted ownership, including administrator edits.
        req.setDeptId(null);
        assertCanAccess(id);
        req.setTenantId(null);
        enforceManagerAssignment(req);
        syncManagerName(req);
    }

    @Override
    public void beforeDelete(List<Long> ids) {
        ids.forEach(this::assertCanAccess);
        referenceGuard.requireDeletable("WAREHOUSE", ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void afterCreate(WarehouseReq req, WarehouseDO entity) {
        clearOtherDefault(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void afterUpdate(WarehouseReq req, WarehouseDO entity) {
        clearOtherDefault(entity);
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        PsiImportHelper.downloadTemplate(response, WarehouseImportRowReq.class, "仓库导入模板.xlsx");
    }

    @Override
    public PsiImportParseResp parseImport(MultipartFile file) {
        CheckUtils.throwIf(file == null || file.isEmpty(), "文件不能为空");
        List<WarehouseImportRowReq> rows;
        try {
            rows = PsiImportHelper.readExcel(file.getInputStream(), WarehouseImportRowReq.class);
        } catch (Exception e) {
            throw new BusinessException("数据文件解析异常，请检查模板格式");
        }
        PsiImportParseResp resp = new PsiImportParseResp();
        resp.setTotalRows(rows.size());
        List<WarehouseImportRowReq> valid = rows.stream()
            .filter(r -> StrUtil.isNotBlank(r.getWarehouseName()))
            .toList();
        CheckUtils.throwIfEmpty(valid, "未解析到有效数据（仓库名称必填）");
        // 表内名称去重
        Set<String> names = new HashSet<>();
        for (WarehouseImportRowReq r : valid) {
            CheckUtils.throwIf(!names.add(r.getWarehouseName().trim()), "表格内仓库名称重复：" + r.getWarehouseName());
        }
        Long tenantId = TenantFillHelper.currentTenantId();
        int dup = 0;
        for (WarehouseImportRowReq r : valid) {
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
        List<WarehouseImportRowReq> rows = PsiImportHelper.loadRows(req.getImportKey(), WarehouseImportRowReq.class);
        PsiImportPolicyEnum policy = req.getDuplicatePolicy() == null ? PsiImportPolicyEnum.SKIP : req.getDuplicatePolicy();
        Long tenantId = TenantFillHelper.currentTenantId();
        int insert = 0;
        int update = 0;
        int skip = 0;
        for (WarehouseImportRowReq row : rows) {
            WarehouseDO exist = findExist(tenantId, row);
            if (exist != null) {
                if (policy == PsiImportPolicyEnum.EXIT) {
                    throw new BusinessException("仓库已存在，已按策略停止导入：" + row.getWarehouseName());
                }
                if (policy == PsiImportPolicyEnum.SKIP) {
                    skip++;
                    continue;
                }
                WarehouseReq updateReq = toReq(row, req.getDefaultStatus());
                updateReq.setWarehouseCode(null);
                this.update(updateReq, exist.getId());
                update++;
            } else {
                this.create(toReq(row, req.getDefaultStatus()));
                insert++;
            }
        }
        PsiImportHelper.clear(req.getImportKey());
        return new PsiImportResp(rows.size(), insert, update, skip);
    }

    private WarehouseDO findExist(Long tenantId, WarehouseImportRowReq row) {
        if (StrUtil.isNotBlank(row.getWarehouseCode())) {
            WarehouseDO byCode = baseMapper.selectOne(Wrappers.lambdaQuery(WarehouseDO.class)
                .eq(WarehouseDO::getTenantId, tenantId)
                .eq(WarehouseDO::getWarehouseCode, row.getWarehouseCode().trim())
                .last("LIMIT 1"));
            if (byCode != null) {
                return byCode;
            }
        }
        return baseMapper.selectOne(Wrappers.lambdaQuery(WarehouseDO.class)
            .eq(WarehouseDO::getTenantId, tenantId)
            .eq(WarehouseDO::getWarehouseName, row.getWarehouseName().trim())
            .last("LIMIT 1"));
    }

    private WarehouseReq toReq(WarehouseImportRowReq row, Integer defaultStatus) {
        WarehouseReq req = new WarehouseReq();
        req.setWarehouseCode(StrUtil.blankToDefault(StrUtil.trim(row.getWarehouseCode()), null));
        req.setWarehouseName(StrUtil.trim(row.getWarehouseName()));
        // 导入仅有姓名时，负责人用户默认落为当前用户，姓名保留导入值
        req.setManagerUserId(UserContextHolder.getUserId());
        req.setManager(StrUtil.trim(row.getManager()));
        if (StrUtil.isBlank(req.getManager())) {
            syncManagerName(req);
        }
        req.setAddress(StrUtil.trim(row.getAddress()));
        req.setIsDefault(PsiImportHelper.parseYesNo(row.getIsDefault(), false));
        req.setStatus(PsiImportHelper.parseStatus(row.getStatus(), defaultStatus));
        req.setRemark(StrUtil.trim(row.getRemark()));
        return req;
    }

    private void clearOtherDefault(WarehouseDO entity) {
        if (!BooleanUtil.isTrue(entity.getIsDefault())) {
            return;
        }
        baseMapper.update(null, Wrappers.lambdaUpdate(WarehouseDO.class)
            .set(WarehouseDO::getIsDefault, false)
            .eq(WarehouseDO::getTenantId, entity.getTenantId())
            .ne(WarehouseDO::getId, entity.getId())
            .eq(WarehouseDO::getIsDefault, true));
    }

    private void syncManagerName(WarehouseReq req) {
        if (req.getManagerUserId() == null) {
            return;
        }
        String nick = UserContextHolder.getNickname(req.getManagerUserId());
        if (StrUtil.isNotBlank(nick)) {
            req.setManager(nick);
        }
    }

    /** 非超管/租户管理员只能把自己设为负责人，避免建完仓后立即失联 */
    private void enforceManagerAssignment(WarehouseReq req) {
        if (!shouldFilterByManager()) {
            return;
        }
        Long userId = UserContextHolder.getUserId();
        CheckUtils.throwIfNull(userId, "无法识别当前用户");
        req.setManagerUserId(userId);
    }

    /** 非超管/租户管理员仅可见自己负责的仓库 */
    private boolean shouldFilterByManager() {
        return !UserContextHolder.isSuperAdmin() && !UserContextHolder.isTenantAdmin();
    }

    private void applyManagerScope(QueryWrapper<WarehouseDO> wrapper) {
        if (!shouldFilterByManager()) {
            return;
        }
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            wrapper.eq("manager_user_id", userId);
        }
    }

    private void assertCanAccess(Long id) {
        if (!shouldFilterByManager()) {
            return;
        }
        WarehouseDO wh = baseMapper.selectById(id);
        CheckUtils.throwIfNull(wh, "仓库不存在");
        CheckUtils.throwIf(!ObjectUtil.equal(wh.getManagerUserId(), UserContextHolder.getUserId()), "只能查看自己负责的仓库");
    }
}
