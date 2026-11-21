package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.biz.sign.domain.SignCert;
import com.ls.aegis.biz.sign.domain.SignKeyStore;
import com.ls.aegis.biz.sign.domain.SignSeal;
import com.ls.aegis.biz.sign.domain.SignSealExtract;
import com.ls.aegis.biz.sign.domain.vo.ManagedCertBundle;
import com.ls.aegis.biz.sign.mapper.SignCertMapper;
import com.ls.aegis.biz.sign.mapper.SignKeyStoreMapper;
import com.ls.aegis.biz.sign.mapper.SignRecordMapper;
import com.ls.aegis.biz.sign.mapper.SignSealMapper;
import com.ls.aegis.biz.sign.mapper.SignSealExtractMapper;
import com.ls.aegis.biz.sign.mapper.SignSealPermissionMapper;
import com.ls.aegis.biz.sign.service.ICertificateService;
import com.ls.aegis.biz.sign.service.ISignSealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 印章管理服务实现
 */
@Service
public class SignSealServiceImpl implements ISignSealService {

    @Autowired
    private SignSealMapper sealMapper;

    @Autowired
    private SignSealPermissionMapper permissionMapper;

    @Autowired
    private SignSealExtractMapper extractMapper;

    @Autowired
    private SignCertMapper signCertMapper;

    @Autowired
    private SignKeyStoreMapper signKeyStoreMapper;

    @Autowired
    private ICertificateService certificateService;

    @Autowired
    private SignRecordMapper signRecordMapper;

    // ==========================================
    //  查询类方法：返回前强制脱敏敏感字段
    // ==========================================

    @Override
    public List<SignSeal> selectSealList(SignSeal seal) {
        List<SignSeal> list = sealMapper.selectSealList(seal);
        if (list != null) {
            list.forEach(this::clearSensitiveFields);
        }
        return list;
    }

    @Override
    public SignSeal selectSealById(Long sealId) {
        SignSeal seal = sealMapper.selectSealById(sealId);
        clearSensitiveFields(seal);
        return seal;
    }

    @Override
    public List<SignSeal> selectAvailableSealList(Long userId, Long projectId) {
        List<SignSeal> list = sealMapper.selectAvailableSealList(userId, projectId);
        if (list != null) {
            list.forEach(this::clearSensitiveFields);
        }
        return list;
    }

    // ==========================================
    //  写入类方法：越权校验 + 敏感字段保护
    // ==========================================

    @Override
    @Transactional
    public int insertSeal(SignSeal seal) {
        // 1. 前端不允许伪造创建人/创建时间/删除标记，一律强制覆盖
        seal.setCreateBy(UserContextHolder.getUsername());
        seal.setCreateTime(LocalDateTime.now());
        seal.setUpdateTime(LocalDateTime.now());
        seal.setDelFlag("0");
        seal.setUpdateBy(null); // 新增时不允许指定更新人

        // 2. 强制忽略前端传入的敏感内部字段（防止伪造托管证书引用）
        seal.setCertId(null);
        seal.setKeyRef(null);
        seal.setCertBindMode(null);

        // 3. 生成印章编号
        if (StringUtils.isBlank(seal.getSealCode())) {
            String nextCode = null;
            try {
                nextCode = sealMapper.selectNextSealCodeWithSequence();
            } catch (Exception e) {
                nextCode = sealMapper.selectNextSealCode();
            }
            seal.setSealCode(nextCode);
        }
        // 默认启用
        if (StringUtils.isNull(seal.getStatus())) {
            seal.setStatus("0");
        }
        return sealMapper.insertSeal(seal);
    }

    @Override
    @Transactional
    public int updateSeal(SignSeal seal) {
        // 【越权校验 1】sealId 不能为空
        if (seal == null || seal.getSealId() == null) {
            throw new ServiceException("印章ID不能为空");
        }
        SignSeal existing = requireSeal(seal.getSealId());

        // 【越权校验 2】仅 管理员 OR 印章创建人 允许修改
        assertCanModifySeal(existing, "修改");

        // 【安全保护】禁止前端通过更新接口替换托管证书引用（只能通过 bindCert 授权流程）
        seal.setCertId(null);
        seal.setKeyRef(null);
        seal.setCertBindMode(null);
        // 【安全保护】禁止通过更新接口改 createBy/createTime/delFlag
        seal.setCreateBy(null);
        seal.setCreateTime(null);
        seal.setDelFlag(null);

        seal.setUpdateBy(UserContextHolder.getUsername());
        seal.setUpdateTime(LocalDateTime.now());
        return sealMapper.updateSeal(seal);
    }

    @Override
    @Transactional
    public int deleteSealByIds(Long[] sealIds) {
        if (sealIds == null || sealIds.length == 0) {
            return 0;
        }
        // 1. 批量越权校验：每一枚印章都必须是创建人或管理员才能删
        // All signing transactions use this same anchor until their audit record commits.
        Long[] ordered = Arrays.stream(sealIds).filter(Objects::nonNull).distinct().sorted().toArray(Long[]::new);
        if (ordered.length != sealIds.length) throw new ServiceException("印章编号重复或无效，请刷新后重试");
        for (Long sealId : ordered) {
            if (sealId == null) {
                continue;
            }
            SignSeal existing = sealMapper.selectSealByIdForUpdate(sealId);
            if (existing == null) throw new ServiceException("印章不存在或已删除");
            assertCanModifySeal(existing, "删除");
        }

        // 2. 关联数据校验：如果印章已被用于实际签章 → 禁止删除（合规要求：签章可追溯）
        long boundSignRecords = signRecordMapper.selectRecordIdsBySealIdsForUpdate(ordered).size();
        if (boundSignRecords > 0) {
            throw new ServiceException(
                    "所选印章中已有 " + boundSignRecords + " 条签章记录，"
                            + "为保障审计链路完整性，已使用的印章不允许删除（建议改为『禁用』）");
        }

        // 3. 执行逻辑删除（del_flag='2'）— 物理删除绝对禁止
        return sealMapper.deleteSealByIds(sealIds);
    }

    // ==========================================
    //  安全辅助方法
    // ==========================================

    /**
     * 判断当前登录人是否允许修改/删除指定印章。
     * 规则：管理员 OR 印章创建人 createBy。
     * 越权则直接抛 ServiceException（Controller 统一返回 HTTP 500+业务消息）。
     */
    private void assertCanModifySeal(SignSeal seal, String action) {
        Long currentUserId = UserContextHolder.getUserId();
        String currentUsername = UserContextHolder.getUsername();

        // 管理员：无条件放行
        if (isAdminUser(currentUserId)) {
            return;
        }
        // 创建人：允许
        String createBy = seal.getCreateBy();
        if (StringUtils.isNotEmpty(createBy) && createBy.equals(currentUsername)) {
            return;
        }
        // 部门管理员/印章授权用户：此处不开放（遵循「最小权限」）
        throw new ServiceException("越权操作：您无权" + action + "印章【" + seal.getSealName() + "】"
                + "，仅管理员或印章创建人可执行此操作");
    }

    /**
     * 脱敏：返回前端前清除不应暴露给客户端的敏感字段。
     * <p>
     * 清除清单：
     * 1) certPassword — 证书密码密文（就算是密文也不能给前端）
     * 2) keyPath      — 私钥本地/对象存储路径
     * 3) keyRef       — 托管证书内部引用编号
     * 4) certPath     — 证书二进制文件存储路径（仍可通过 certInfo 看摘要信息）
     * 保留：certInfo（证书主题/有效期/指纹摘要）、certId、certBindMode、status
     */
    private void clearSensitiveFields(SignSeal seal) {
        if (seal == null) {
            return;
        }
        seal.setCertPassword(null);
        seal.setKeyPath(null);
        seal.setKeyRef(null);
        seal.setCertPath(null);
    }

    @Override
    @Transactional
    public int bindPermissions(Long sealId, Long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            return 0;
        }
        List<Long> existingUserIds = permissionMapper.selectUserIdsBySealId(sealId);
        List<Long> toInsertUserIds = Arrays.stream(userIds)
                .filter(Objects::nonNull)
                .distinct()
                .filter(userId -> existingUserIds == null || !existingUserIds.contains(userId))
                .collect(Collectors.toList());
        if (toInsertUserIds.isEmpty()) {
            return 1;
        }
        return permissionMapper.batchInsert(sealId, toInsertUserIds.toArray(new Long[0]));
    }

    @Override
    @Transactional
    public int removePermissions(Long sealId, Long[] userIds) {
        return permissionMapper.deleteBySealIdAndUserIds(sealId, userIds);
    }

    @Override
    public List<Long> selectPermittedUserIds(Long sealId) {
        return permissionMapper.selectUserIdsBySealId(sealId);
    }

    @Override
    public boolean canAccessSealImage(Long sealId, Long userId) {
        SignSeal seal = sealMapper.selectSealById(sealId);
        if (seal == null || !"0".equals(seal.getStatus())) {
            return false;
        }
        if (isAdminUser(userId)) {
            return true;
        }
        return permissionMapper.existsPermission(sealId, userId) > 0;
    }

    @Override
    public boolean canAccessExtractRecord(Long recordId, Long userId) {
        SignSealExtract record = extractMapper.selectExtractById(recordId);
        if (record == null) {
            return false;
        }
        if (isAdminUser(userId)) {
            return true;
        }
        String username = UserContextHolder.getUsername();
        String nickName = UserContextHolder.getNickname(userId);
        return StringUtils.isNotEmpty(record.getCreateBy())
                && (record.getCreateBy().equals(username) || record.getCreateBy().equals(nickName));
    }

    private static boolean isAdminUser(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            var ctx = UserContextHolder.getContext();
            if (ctx != null && userId.equals(ctx.getId())) {
                return ctx.isSuperAdmin();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return Long.valueOf(1L).equals(userId);
    }

    @Override
    @Transactional
    public Map<String, Object> bindSealCert(Long sealId) {
        SignSeal seal = requireSeal(sealId);
        SignCert cert = signCertMapper.selectActiveBySealId(sealId);
        SignKeyStore keyStore = loadManagedKeyStore(cert);
        if (cert == null || keyStore == null) {
            try {
                if (cert != null && cert.getCertId() != null) {
                    signCertMapper.updateSignCertStatus(cert.getCertId(), "1");
                }
                String companyName = StringUtils.isNotEmpty(seal.getOrgName()) ? seal.getOrgName() : seal.getSealName();
                ManagedCertBundle bundle = certificateService.issueManagedCert(sealId, seal.getSealName(), companyName);
                cert = bundle.getCert();
                keyStore = bundle.getKeyStore();
                signCertMapper.insertSignCert(cert);
                signKeyStoreMapper.insertSignKeyStore(keyStore);
            } catch (Exception e) {
                throw new ServiceException("签发平台证书失败: " + e.getMessage());
            }
        }

        SignSeal update = new SignSeal();
        update.setSealId(sealId);
        update.setCertId(cert.getCertId());
        update.setKeyRef(cert.getKeyRef());
        update.setCertBindMode("managed");
        sealMapper.updateSeal(update);

        seal.setCertId(cert.getCertId());
        seal.setKeyRef(cert.getKeyRef());
        seal.setCertBindMode("managed");
        return buildSealCertInfo(seal, cert);
    }

    private SignKeyStore loadManagedKeyStore(SignCert cert) {
        if (cert == null || StringUtils.isEmpty(cert.getKeyRef())) {
            return null;
        }
        SignKeyStore keyStore = signKeyStoreMapper.selectByKeyRef(cert.getKeyRef());
        if (keyStore == null || StringUtils.isEmpty(keyStore.getEncryptedKeyBlob()) || "1".equals(keyStore.getStatus())) {
            return null;
        }
        return keyStore;
    }

    @Override
    public Map<String, Object> getSealCertInfo(Long sealId) {
        SignSeal seal = requireSeal(sealId);
        SignCert cert = null;
        if (seal.getCertId() != null) {
            cert = signCertMapper.selectSignCertByCertId(seal.getCertId());
        }
        if (cert == null) {
            cert = signCertMapper.selectActiveBySealId(sealId);
        }
        return buildSealCertInfo(seal, cert);
    }

    private SignSeal requireSeal(Long sealId) {
        SignSeal seal = sealMapper.selectSealById(sealId);
        if (seal == null) {
            throw new ServiceException("印章不存在");
        }
        return seal;
    }

    private Map<String, Object> buildSealCertInfo(SignSeal seal, SignCert cert) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean bound = cert != null || seal.getCertId() != null || StringUtils.isNotEmpty(seal.getKeyRef());
        result.put("sealId", seal.getSealId());
        result.put("sealName", seal.getSealName());
        result.put("bound", bound);
        result.put("certId", cert != null ? cert.getCertId() : seal.getCertId());
        result.put("keyRef", cert != null ? cert.getKeyRef() : seal.getKeyRef());
        result.put("certBindMode", StringUtils.isNotEmpty(seal.getCertBindMode()) ? seal.getCertBindMode() : (bound ? "managed" : null));
        result.put("serialNo", cert != null ? cert.getSerialNo() : null);
        result.put("subjectDn", cert != null ? cert.getSubjectDn() : null);
        result.put("issuerDn", cert != null ? cert.getIssuerDn() : null);
        result.put("fingerprintSha256", cert != null ? cert.getFingerprintSha256() : null);
        result.put("notBefore", cert != null ? cert.getNotBefore() : null);
        result.put("notAfter", cert != null ? cert.getNotAfter() : null);
        result.put("certStatus", resolveCertStatus(seal, cert, bound));
        return result;
    }

    private String resolveCertStatus(SignSeal seal, SignCert cert, boolean bound) {
        if (!bound) {
            return "未绑定";
        }
        if (cert == null) {
            return "绑定异常";
        }
        Date now = new Date();
        if ("1".equals(cert.getStatus())) {
            return "已禁用";
        }
        if (cert.getRevokedAt() != null) {
            return "已吊销";
        }
        if (cert.getNotAfter() != null && cert.getNotAfter().before(now)) {
            return "已过期";
        }
        if (cert.getNotBefore() != null && cert.getNotBefore().after(now)) {
            return "未生效";
        }
        if (certificateService != null && certificateService.ensureCertUsable(cert)) {
            return "已绑定";
        }
        return StringUtils.isNotEmpty(seal.getKeyRef()) ? "已绑定" : "绑定异常";
    }
}
