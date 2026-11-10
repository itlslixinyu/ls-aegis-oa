package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.enums.PsiImportPolicyEnum;
import com.ls.aegis.biz.psi.mapper.CompanyAccountMapper;
import com.ls.aegis.biz.psi.model.entity.CompanyAccountDO;
import com.ls.aegis.biz.psi.model.query.CompanyAccountQuery;
import com.ls.aegis.biz.psi.model.req.AccountImportRowReq;
import com.ls.aegis.biz.psi.model.req.CompanyAccountReq;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.resp.CompanyAccountDetailResp;
import com.ls.aegis.biz.psi.model.resp.CompanyAccountResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.service.CompanyAccountService;
import com.ls.aegis.biz.psi.support.AccountNoMask;
import com.ls.aegis.biz.psi.support.PsiImportHelper;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.mybatis.base.service.BaseServiceImpl;
import com.ls.aegis.crypto.encryptor.Sm4Encryptor;
import top.continew.starter.encrypt.context.CryptoContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.CheckUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CompanyAccountServiceImpl extends BaseServiceImpl<CompanyAccountMapper, CompanyAccountDO, CompanyAccountResp, CompanyAccountDetailResp, CompanyAccountQuery, CompanyAccountReq> implements CompanyAccountService {

    @Override
    public void beforeCreate(CompanyAccountReq req) {
        validateAccount(req);
        req.setTenantId(TenantFillHelper.currentTenantId());
        req.setDeptId(TenantFillHelper.currentDeptId());
    }

    @Override
    public void beforeUpdate(CompanyAccountReq req, Long id) {
        validateAccount(req);
        req.setTenantId(null);
        req.setDeptId(null);
    }

    private static void validateAccount(CompanyAccountReq req) {
        if (StrUtil.isBlank(req.getAccountName()) || StrUtil.isBlank(req.getBankName()) || StrUtil.isBlank(req.getAccountNo())) {
            throw new BusinessException("户名、开户行和账号不能为空");
        }
        // account_no stores at most 128 Base64 characters: SM4/PKCS7 permits at most 95 UTF-8 bytes.
        if (req.getAccountNo().getBytes(StandardCharsets.UTF_8).length > 95) {
            throw new BusinessException("账号内容超出加密存储长度限制");
        }
    }

    /**
     * 列表脱敏账号；详情（CompanyAccountDetailResp）保持明文便于编辑。
     */
    @Override
    protected void fill(Object obj) {
        super.fill(obj);
        if (obj instanceof CompanyAccountResp resp && !(obj instanceof CompanyAccountDetailResp)) {
            resp.setAccountNo(AccountNoMask.mask(resp.getAccountNo()));
        }
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        PsiImportHelper.downloadTemplate(response, AccountImportRowReq.class, "公司账户导入模板.xlsx");
    }

    @Override
    public PsiImportParseResp parseImport(MultipartFile file) {
        CheckUtils.throwIf(file == null || file.isEmpty(), "文件不能为空");
        List<AccountImportRowReq> rows;
        try {
            rows = PsiImportHelper.readExcel(file.getInputStream(), AccountImportRowReq.class);
        } catch (Exception e) {
            throw new BusinessException("数据文件解析异常，请检查模板格式");
        }
        PsiImportParseResp resp = new PsiImportParseResp();
        resp.setTotalRows(rows.size());
        List<AccountImportRowReq> valid = rows.stream().filter(r -> StrUtil.isNotBlank(r.getAccountName())).toList();
        CheckUtils.throwIfEmpty(valid, "未解析到有效数据（户名必填）");
        Set<String> names = new HashSet<>();
        for (AccountImportRowReq r : valid) {
            CheckUtils.throwIf(!names.add(r.getAccountName().trim()), "表格内户名重复：" + r.getAccountName());
        }
        Long tenantId = TenantFillHelper.currentTenantId();
        int dup = 0;
        for (AccountImportRowReq r : valid) {
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
        List<AccountImportRowReq> rows = PsiImportHelper.loadRows(req.getImportKey(), AccountImportRowReq.class);
        PsiImportPolicyEnum policy = req.getDuplicatePolicy() == null ? PsiImportPolicyEnum.SKIP : req.getDuplicatePolicy();
        Long tenantId = TenantFillHelper.currentTenantId();
        int insert = 0;
        int update = 0;
        int skip = 0;
        for (AccountImportRowReq row : rows) {
            CompanyAccountDO exist = findExist(tenantId, row);
            if (exist != null) {
                if (policy == PsiImportPolicyEnum.EXIT) {
                    throw new BusinessException("账户已存在，已按策略停止导入：" + row.getAccountName());
                }
                if (policy == PsiImportPolicyEnum.SKIP) {
                    skip++;
                    continue;
                }
                this.update(toReq(row, req.getDefaultStatus()), exist.getId());
                update++;
            } else {
                this.create(toReq(row, req.getDefaultStatus()));
                insert++;
            }
        }
        PsiImportHelper.clear(req.getImportKey());
        return new PsiImportResp(rows.size(), insert, update, skip);
    }

    private CompanyAccountDO findExist(Long tenantId, AccountImportRowReq row) {
        if (StrUtil.isNotBlank(row.getAccountNo())) {
            // ContiNew's query interceptor skips AbstractWrapper parameters. Match the same deterministic
            // SM4 ciphertext used by @FieldEncrypt on writes; use the shared encryptor/key provider.
            String encryptedNo = new Sm4Encryptor(new CryptoContext()).encrypt(row.getAccountNo().trim());
            CompanyAccountDO byNo = baseMapper.selectOne(Wrappers.lambdaQuery(CompanyAccountDO.class)
                .eq(CompanyAccountDO::getTenantId, tenantId)
                .eq(CompanyAccountDO::getAccountNo, encryptedNo)
                .last("LIMIT 1"));
            if (byNo != null) {
                return byNo;
            }
        }
        return baseMapper.selectOne(Wrappers.lambdaQuery(CompanyAccountDO.class)
            .eq(CompanyAccountDO::getTenantId, tenantId)
            .eq(CompanyAccountDO::getAccountName, row.getAccountName().trim())
            .last("LIMIT 1"));
    }

    private CompanyAccountReq toReq(AccountImportRowReq row, Integer defaultStatus) {
        CompanyAccountReq req = new CompanyAccountReq();
        req.setAccountName(StrUtil.trim(row.getAccountName()));
        req.setBankName(StrUtil.trim(row.getBankName()));
        req.setAccountNo(StrUtil.trim(row.getAccountNo()));
        req.setStatus(PsiImportHelper.parseStatus(row.getStatus(), defaultStatus));
        req.setRemark(StrUtil.trim(row.getRemark()));
        return req;
    }
}
