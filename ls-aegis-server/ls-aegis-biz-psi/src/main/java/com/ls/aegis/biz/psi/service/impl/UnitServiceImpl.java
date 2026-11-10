package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.PsiImportPolicyEnum;
import com.ls.aegis.biz.psi.enums.UnitNatureEnum;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.model.entity.UnitDO;
import com.ls.aegis.biz.psi.model.query.UnitQuery;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.req.UnitImportRowReq;
import com.ls.aegis.biz.psi.model.req.UnitReq;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.model.resp.UnitDetailResp;
import com.ls.aegis.biz.psi.model.resp.UnitResp;
import com.ls.aegis.biz.psi.service.UnitService;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 往来单位业务实现
 */
@Service
@RequiredArgsConstructor
public class UnitServiceImpl extends BaseServiceImpl<UnitMapper, UnitDO, UnitResp, UnitDetailResp, UnitQuery, UnitReq> implements UnitService {

    private final DocNoGenerator docNoGenerator;
    private final com.ls.aegis.biz.psi.support.MasterDataReferenceGuard referenceGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) { super.delete(ids); }

    @Override
    public void beforeDelete(List<Long> ids) { referenceGuard.requireDeletable("UNIT", ids); }

    @Override
    public void beforeCreate(UnitReq req) {
        req.setTenantId(TenantFillHelper.currentTenantId());
        req.setDeptId(TenantFillHelper.currentDeptId());
        if (StrUtil.isBlank(req.getUnitCode())) {
            req.setUnitCode(docNoGenerator.nextCode(req.getTenantId(), PsiConstants.DOC_PREFIX_UNIT));
        }
    }

    @Override
    public void beforeUpdate(UnitReq req, Long id) {
        referenceGuard.requireEditable("UNIT", id);
        // Normal edits cannot move persisted ownership, including administrator edits.
        req.setDeptId(null);
        req.setTenantId(null);
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        PsiImportHelper.downloadTemplate(response, UnitImportRowReq.class, "往来单位导入模板.xlsx");
    }

    @Override
    public PsiImportParseResp parseImport(MultipartFile file) {
        CheckUtils.throwIf(file == null || file.isEmpty(), "文件不能为空");
        List<UnitImportRowReq> rows;
        try {
            rows = PsiImportHelper.readExcel(file.getInputStream(), UnitImportRowReq.class);
        } catch (Exception e) {
            throw new BusinessException("数据文件解析异常，请检查模板格式");
        }
        PsiImportParseResp resp = new PsiImportParseResp();
        resp.setTotalRows(rows.size());
        List<UnitImportRowReq> valid = rows.stream()
            .filter(r -> StrUtil.isNotBlank(r.getUnitName()))
            .toList();
        CheckUtils.throwIfEmpty(valid, "未解析到有效数据（单位全称必填）");
        Set<String> names = new HashSet<>();
        for (UnitImportRowReq r : valid) {
            CheckUtils.throwIf(!names.add(r.getUnitName().trim()), "表格内单位全称重复：" + r.getUnitName());
            parseUnitType(r.getUnitType());
            parseUnitNature(r.getUnitNature());
        }
        Long tenantId = TenantFillHelper.currentTenantId();
        int dup = 0;
        for (UnitImportRowReq r : valid) {
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
        List<UnitImportRowReq> rows = PsiImportHelper.loadRows(req.getImportKey(), UnitImportRowReq.class);
        PsiImportPolicyEnum policy = req.getDuplicatePolicy() == null ? PsiImportPolicyEnum.SKIP : req.getDuplicatePolicy();
        Long tenantId = TenantFillHelper.currentTenantId();
        int insert = 0;
        int update = 0;
        int skip = 0;
        for (UnitImportRowReq row : rows) {
            UnitDO exist = findExist(tenantId, row);
            if (exist != null) {
                if (policy == PsiImportPolicyEnum.EXIT) {
                    throw new BusinessException("往来单位已存在，已按策略停止导入：" + row.getUnitName());
                }
                if (policy == PsiImportPolicyEnum.SKIP) {
                    skip++;
                    continue;
                }
                UnitReq updateReq = toReq(row, req.getDefaultStatus());
                updateReq.setUnitCode(null);
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

    private UnitDO findExist(Long tenantId, UnitImportRowReq row) {
        if (StrUtil.isNotBlank(row.getUnitCode())) {
            UnitDO byCode = baseMapper.selectOne(Wrappers.lambdaQuery(UnitDO.class)
                .eq(UnitDO::getTenantId, tenantId)
                .eq(UnitDO::getUnitCode, row.getUnitCode().trim())
                .last("LIMIT 1"));
            if (byCode != null) {
                return byCode;
            }
        }
        return baseMapper.selectOne(Wrappers.lambdaQuery(UnitDO.class)
            .eq(UnitDO::getTenantId, tenantId)
            .eq(UnitDO::getUnitName, row.getUnitName().trim())
            .last("LIMIT 1"));
    }

    private UnitReq toReq(UnitImportRowReq row, Integer defaultStatus) {
        UnitReq req = new UnitReq();
        req.setUnitCode(StrUtil.blankToDefault(StrUtil.trim(row.getUnitCode()), null));
        req.setUnitName(StrUtil.trim(row.getUnitName()));
        req.setUnitType(parseUnitType(row.getUnitType()));
        req.setUnitNature(parseUnitNature(row.getUnitNature()));
        req.setCreditCode(StrUtil.trim(row.getCreditCode()));
        req.setAddress(StrUtil.trim(row.getAddress()));
        req.setStatus(PsiImportHelper.parseStatus(row.getStatus(), defaultStatus));
        req.setRemark(StrUtil.trim(row.getRemark()));
        return req;
    }

    private UnitTypeEnum parseUnitType(String raw) {
        CheckUtils.throwIfBlank(raw, "单位类型不能为空（客户/供应商/双重身份）");
        String v = raw.trim();
        for (UnitTypeEnum e : UnitTypeEnum.values()) {
            if (e.getValue().equalsIgnoreCase(v) || e.getDescription().equals(v)) {
                return e;
            }
        }
        throw new BusinessException("无效单位类型：" + raw + "（支持：客户/供应商/双重身份）");
    }

    private UnitNatureEnum parseUnitNature(String raw) {
        CheckUtils.throwIfBlank(raw, "单位性质不能为空（企业/个人）");
        String v = raw.trim();
        for (UnitNatureEnum e : UnitNatureEnum.values()) {
            if (e.getValue().equalsIgnoreCase(v) || e.getDescription().equals(v)) {
                return e;
            }
        }
        throw new BusinessException("无效单位性质：" + raw + "（支持：企业/个人）");
    }
}
