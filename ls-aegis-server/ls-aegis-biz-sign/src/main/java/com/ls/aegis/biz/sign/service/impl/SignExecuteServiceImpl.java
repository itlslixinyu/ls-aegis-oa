package com.ls.aegis.biz.sign.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.http.UserAgentUtils;
import com.ls.aegis.compat.common.utils.ip.IpUtils;
import com.ls.aegis.compat.common.utils.ip.AddressUtils;
import com.ls.aegis.compat.common.utils.ServletUtils;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.sign.domain.SignFile;
import com.ls.aegis.biz.sign.domain.SignCert;
import com.ls.aegis.biz.sign.domain.SignLog;
import com.ls.aegis.biz.sign.domain.SignProject;
import com.ls.aegis.biz.sign.domain.SignRecord;
import com.ls.aegis.biz.sign.domain.SignSeal;
import com.ls.aegis.biz.sign.domain.SignVerifyReceipt;
import com.ls.aegis.biz.sign.domain.vo.SignPosition;
import com.ls.aegis.biz.sign.domain.vo.SignRequest;
import com.ls.aegis.biz.sign.domain.vo.SignResult;
import com.ls.aegis.biz.sign.mapper.SignFileMapper;
import com.ls.aegis.biz.sign.mapper.SignCertMapper;
import com.ls.aegis.biz.sign.mapper.SignLogMapper;
import com.ls.aegis.biz.sign.mapper.SignProjectMapper;
import com.ls.aegis.biz.sign.mapper.SignRecordMapper;
import com.ls.aegis.biz.sign.mapper.SignSealMapper;
import com.ls.aegis.biz.sign.mapper.SignSealPermissionMapper;
import com.ls.aegis.biz.sign.mapper.SignVerifyReceiptMapper;
import com.ls.aegis.biz.sign.service.IReceiptService;
import com.ls.aegis.biz.sign.service.ISignSealService;
import com.ls.aegis.biz.sign.service.ISignExecuteService;
import com.ls.aegis.biz.sign.service.impl.SignFileServiceImpl;
import com.ls.aegis.biz.sign.utils.SignDataScopeHelper;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.text.SimpleDateFormat;

/**
 * PDF签章服务实现
 */
@Service
public class SignExecuteServiceImpl implements ISignExecuteService {

    private static final Logger log = LoggerFactory.getLogger(SignExecuteServiceImpl.class);

    @Autowired
    private SignSealMapper sealMapper;

    @Autowired
    private SignFileMapper fileMapper;

    @Autowired
    private SignRecordMapper recordMapper;

    @Autowired
    private SignVerifyReceiptMapper signVerifyReceiptMapper;

    @Autowired
    private SignLogMapper logMapper;

    @Autowired
    private SignCertMapper signCertMapper;

    @Autowired
    private SignProjectMapper projectMapper;

    @Autowired
    private SignPdfServiceImpl pdfService;

    @Autowired
    private SignFileServiceImpl fileService;

    @Autowired
    private CertService certService;

    @Autowired
    private IReceiptService receiptService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SignSealPermissionMapper permissionMapper;

    @Autowired
    private SignRequestNormalizer requestNormalizer;

    @Autowired
    private ISignSealService sealService;

    @Autowired
    private SignSnapshotFingerprintHelper snapshotFingerprintHelper;

    /**
     * 执行PDF签章
     */
    @Override
    @Transactional
    public SignResult executeSign(SignRequest request) {
        Long userId = UserContextHolder.getUserId();
        request.setSignMode(requestNormalizer.normalizeMode(request.getSignMode()));
        validateSingleModeOnly(request);
        // 校验请求并获取印章对象（避免重复查询数据库）
        SignSeal seal = validateExecutableRequest(request, userId, true);

        request.setFileId(fileService.resolveToSignFileId(request.getFileId(), request.getSysFileId()));

        // 1. 如果有印章（非纯时间戳模式），确保托管证书的印章就绪
        boolean isTimestampOnly = requestNormalizer.isTimestampOnlyRequest(request);
        if (!isTimestampOnly && seal != null) {
            if (shouldUseManagedKey(request, seal)) {
                seal = ensureManagedSealReady(seal);
            }
        }

        // 3. 加载源文件
        SignFile sourceFile = fileMapper.selectFileById(request.getFileId());
        if (sourceFile == null) {
            throw new ServiceException("源文件不存在");
        }

        // 4. 读取源文件字节
        byte[] sourceBytes;
        try {
            sourceBytes = fileService.getFileBytes(request.getFileId());
        } catch (Exception e) {
            throw new ServiceException("无法读取源文件内容: " + e.getMessage());
        }
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw new ServiceException("源文件内容为空");
        }

        int totalPages = countPdfPages(sourceBytes);
        java.util.List<SignPosition> normalizedPositions = requestNormalizer.normalizePositions(request, totalPages);

        if (seal != null && seal.getSealFilePath() != null) {
            String realSealPath = seal.getSealFilePath();
            for (SignPosition pos : normalizedPositions) {
                if (pos.getSealFilePath() == null || pos.getSealFilePath().startsWith("/sign/")) {
                    pos.setSealFilePath(realSealPath);
                }
            }
        }

        request.setPositions(normalizedPositions);

        // 5. 计算源文件SHA256
        String fileHash = sha256(sourceBytes);

        try {
            // 6. 执行PDF签章（核心逻辑）
            byte[] signedBytes = shouldUseManagedKey(request, seal)
                    ? pdfService.signWithManagedCert(sourceBytes, seal, request)
                    : pdfService.sign(sourceBytes, seal, request);

            // 7. 计算签章后SHA256
            String signedHash = sha256(signedBytes);

            // 8. 上传到 MinIO
            Date signTime = new Date();
            String signTimeStr = new SimpleDateFormat("yyyyMMddHHmmss").format(signTime);
            String signModeName = getSignModeName(request.getSignMode());
            String originalName = sourceFile.getFileName();
            int dotIdx = originalName.lastIndexOf('.');
            String baseName = dotIdx > 0 ? originalName.substring(0, dotIdx) : originalName;
            String ext = dotIdx > 0 ? originalName.substring(dotIdx) : ".pdf";
            String displayFileName = baseName + "_" + signModeName + "_" + signTimeStr + ext;
            SignFile signedFile = fileService.uploadBytes(signedBytes, displayFileName, "sign");

            // 9. 生成记录ID
            String recordId = UUID.randomUUID().toString().replace("-", "");

            // 10. 记录签章记录
            SignRecord record = new SignRecord();
            record.setRecordId(recordId);
            record.setFileId(request.getFileId());
            record.setSealId(request.getSealId());
            record.setUserId(userId);
            record.setProjectId(request.getProjectId());
            record.setSignMode(request.getSignMode());
            record.setPositions(objectMapper.writeValueAsString(request.getPositions()));
            record.setHasTimestamp(request.getHasTimestamp() != null ? request.getHasTimestamp() : "0");
            if (request.getSpanConfig() != null) {
                record.setSpanConfig(objectMapper.writeValueAsString(request.getSpanConfig()));
            }
            record.setClientIp(IpUtils.getIpAddr());
            record.setIpLocation(AddressUtils.getRealAddressByIP(IpUtils.getIpAddr()));
            // 从请求头获取 User-Agent 解析浏览器和操作系统
            String userAgent = "";
            try {
                userAgent = ServletUtils.getRequest().getHeader("User-Agent");
            } catch (Exception e) {
                // 非 Web 上下文，忽略
            }
            record.setOsVersion(UserAgentUtils.getOperatingSystem(userAgent));
            record.setBrowserVersion(UserAgentUtils.getBrowser(userAgent));
            record.setStatus("completed");
            record.setSignedFileId(signedFile.getFileId());
            record.setCreateTime(XmlMapperDO.from(signTime));
            if (shouldUseManagedKey(request, seal)) {
                applyManagedCertSnapshot(record, seal, signTime);
            } else {
                // P0-6: sign_time 改由数据库 CURRENT_TIMESTAMP(3) 自动填充
                // 统一以 DB 服务器时间为准，避免应用服务器与 DB 时间偏差
            }
            SignProject project = request.getProjectId() != null
                ? projectMapper.selectProjectById(request.getProjectId()) : null;
            SignDataScopeHelper.fillAuditRow(record, userId, project);
            recordMapper.insertRecord(record);
            persistSnapshotHashes(record);
            createReceiptSnapshot(record);

            // 11. 记录签章日志
            SignLog signLog = new SignLog();
            signLog.setProjectId(request.getProjectId());
            signLog.setSealId(request.getSealId());
            signLog.setUserId(userId);
            signLog.setFileHash(fileHash);
            signLog.setClientIp(IpUtils.getIpAddr());
            signLog.setIpLocation(AddressUtils.getRealAddressByIP(IpUtils.getIpAddr()));
            signLog.setActionType("SIGN_ADD");
            signLog.setResultCode("SUCCESS");
            signLog.setOutputFileId(signedFile.getFileId());
            signLog.setSignTime(signTime);
            signLog.setCreateTime(XmlMapperDO.from(signTime));
            SignDataScopeHelper.fillAuditRow(signLog, userId, project);
            logMapper.insertLog(signLog);

            log.info("签章成功: recordId={}, userId={}, sealId={}, file={}",
                    recordId, userId, request.getSealId(), sourceFile.getFileName());

            // 12. 返回结果
            SignResult result = new SignResult();
            result.setRecordId(recordId);
            result.setSignedFileId(signedFile.getFileId());
            result.setSignedFileName(signedFile.getFileName());
            result.setDownloadUrl("/sign/file/download/" + signedFile.getFileId());
            result.setSignTime(signTime);

            return result;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("签章执行失败", e);
            throw new ServiceException("签章执行失败: " + e.getMessage());
        }
    }

    @Override
    public String extractCertInfo(Long fileId) {
        try {
            SignFile file = fileMapper.selectFileById(fileId);
            if (file == null) {
                throw new ServiceException("文件不存在");
            }

            byte[] bytes = fileService.getFileBytes(fileId);
            return pdfService.extractCertInfo(bytes);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("提取证书信息失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Override
    public boolean verifyFingerprint(Long fileId) {
        try {
            // 1. 获取文件字节
            byte[] bytes = fileService.getFileBytes(fileId);
            String currentHash = sha256(bytes);

            // 2. 查询签章记录中的原始哈希
            // 方式一：从 sign_log 表查找该文件对应的源文件哈希
            // 方式二：从 sign_record 中查找该 signedFileId 对应的源文件哈希
            SignFile signedFile = fileMapper.selectFileById(fileId);
            if (signedFile == null) {
                return false;
            }

            // TODO: 通过 signedFileId 反查源文件记录
            // SELECT file_hash FROM sign_log WHERE output_file_id = #{fileId} LIMIT 1
            // 如果找到了 originalHash，比对 sha256(signedBytes) == originalHash
            // 注意：签章后文件哈希必然变化，所以应验证签名而非哈希

            // 正确做法：验证 PKCS7 签名有效性
            return pdfService.verifySignature(bytes);
        } catch (Exception e) {
            log.error("指纹校验失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> verifyFingerprintUpload(MultipartFile file) {
        Map<String, Object> resp = new LinkedHashMap<>();
        Map<String, Object> fp = new LinkedHashMap<>();
        Map<String, Object> ca = new LinkedHashMap<>();
        resp.put("valid", false);
        resp.put("signaturePresent", false);
        // Signature integrity is checked here; no configured trust store or revocation check is performed.
        resp.put("trustVerified", false);

        if (file == null || file.isEmpty()) {
            fp.put("result", "校验失败");
            fp.put("resultType", "danger");
            fp.put("compareTip", "上传文件为空");
            ca.put("result", "校验失败");
            ca.put("resultType", "danger");
            resp.put("valid", false);
            resp.put("message", "上传文件为空");
            resp.put("fingerprint", fp);
            resp.put("ca", ca);
            return resp;
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!originalName.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            fp.put("result", "校验失败");
            fp.put("resultType", "danger");
            fp.put("compareTip", "请上传 PDF 文件");
            ca.put("result", "校验失败");
            ca.put("resultType", "danger");
            resp.put("valid", false);
            resp.put("message", "文件类型错误");
            resp.put("fingerprint", fp);
            resp.put("ca", ca);
            return resp;
        }

        try {
            byte[] bytes = file.getBytes();
            String currentHash = sha256(bytes);

            // Read format/signature presence before assigning a verification outcome.
            Map<String, String> meta = pdfService.readMetadata(bytes);
            if (Integer.parseInt(meta.getOrDefault("pageCount", "0")) < 1) {
                throw new IllegalArgumentException("Invalid PDF");
            }
            boolean signaturePresent = Integer.parseInt(meta.getOrDefault("signatureCount", "0")) > 0;
            resp.put("signaturePresent", signaturePresent);
            boolean valid = signaturePresent && pdfService.verifySignature(bytes);
            String author = meta.getOrDefault("author", "-");
            String creator = meta.getOrDefault("creator", "-");
            String producer = meta.getOrDefault("producer", "-");
            String created = meta.getOrDefault("created", "-");
            String modified = meta.getOrDefault("modified", "-");
            String signTime = meta.getOrDefault("signTime", "-");
            String certSerial = meta.getOrDefault("certSerial", "-");
            String certIssuer = meta.getOrDefault("certIssuer", "-");
            String certValidFrom = meta.getOrDefault("certValidFrom", "-");
            String certValidTo = meta.getOrDefault("certValidTo", "-");
            String certFingerprint = meta.getOrDefault("certFingerprint", "-");
            String signCertFingerprint = meta.getOrDefault("signCertFingerprint", "-");
            String pageFingerprint = meta.getOrDefault("pageFingerprint", "-");
            String fingerprintSource = meta.getOrDefault("fingerprintSource", "PDF 元数据");
            String originalPdfHash = meta.getOrDefault("originalPdfHash", "-");
            String sealHash = meta.getOrDefault("sealHash", "-");
            SignFile signedFile = fileMapper.selectFileBySha256(currentHash);
            if (signedFile != null && signedFile.getFileId() != null) {
                SignRecord record = recordMapper.selectBySignedFileId(signedFile.getFileId());
                if ((signCertFingerprint == null || "-".equals(signCertFingerprint)) && record != null
                        && StringUtils.isNotEmpty(record.getFingerprintSha256())) {
                    signCertFingerprint = record.getFingerprintSha256();
                }
                if ((sealHash == null || "-".equals(sealHash)) && record != null
                        && StringUtils.isNotEmpty(record.getSignHash())) {
                    sealHash = record.getSignHash();
                }
                if ((pageFingerprint == null || "-".equals(pageFingerprint)) && record != null
                        && StringUtils.isNotEmpty(record.getPageFingerprint())) {
                    pageFingerprint = record.getPageFingerprint();
                }
                if (record != null && StringUtils.isNotEmpty(record.getRecordId())) {
                    resp.put("recordId", record.getRecordId());
                }
                SignLog signLog = logMapper.selectLatestByOutputFileId(signedFile.getFileId());
                if ((originalPdfHash == null || "-".equals(originalPdfHash)) && signLog != null
                        && StringUtils.isNotEmpty(signLog.getFileHash())) {
                    originalPdfHash = signLog.getFileHash();
                }
            }

            // 3. 填充指纹校验
            fp.put("result", valid ? "签名完整性校验通过" : signaturePresent ? "签名校验未通过" : "未检测到数字签名");
            fp.put("resultType", valid ? "success" : signaturePresent ? "danger" : "warning");
            fp.put("user", author.equals("-") ? "-" : author);
            fp.put("timestamp", signTime.equals("-") ? created : signTime);
            fp.put("originalPdfHash", originalPdfHash);
            fp.put("sealHash", sealHash);
            fp.put("currentFileHash", currentHash);
            fp.put("pageFingerprint", pageFingerprint);
            fp.put("fingerprintSource", fingerprintSource);
            fp.put("compareTip", valid ? "数字签名完整性校验通过；证书信任链及撤销状态未验证"
                    : signaturePresent ? "签名完整性或签署时证书有效期校验未通过，请核实文件和证书"
                    : "文件未包含数字签名，无法验证签名完整性");

            // 4. 填充 CA 校验
            boolean hasCertificate = !certSerial.equals("-") && !certIssuer.equals("-");
            ca.put("result", hasCertificate ? "信任链未验证" : "缺少对比数据");
            ca.put("resultType", "warning");
            ca.put("signer", meta.getOrDefault("signer", "-"));
            ca.put("signTime", signTime.equals("-") ? created : signTime);
            ca.put("currentFileHash", currentHash);
            ca.put("certSerial", certSerial);
            ca.put("certIssuer", certIssuer);
            ca.put("certValid", certValidFrom.equals("-") ? "-" : (certValidFrom + " ~ " + certValidTo));
            ca.put("certFingerprint", certFingerprint);
            ca.put("signCertFingerprint", signCertFingerprint);

            resp.put("valid", valid);
            resp.put("message", fp.get("result"));
        } catch (Exception e) {
            log.warn("指纹上传校验未完成: {}", e.getClass().getSimpleName());
            fp.put("result", "校验失败");
            fp.put("resultType", "danger");
            fp.put("compareTip", "PDF解析或验签失败，请确认文件完整且未加密");
            ca.put("result", "校验失败");
            ca.put("resultType", "danger");
            resp.put("valid", false);
            resp.put("message", "PDF解析或验签失败，请确认文件完整且未加密");
        }

        resp.put("fingerprint", fp);
        resp.put("ca", ca);
        return resp;
    }

    @Override
    public Object getSignableProjects(Long userId) {
        projectMapper.refreshExpiredProjects();
        // 可签列表：仅「进行中」，待审核/未开始/已完成/已暂停/已取消均不出现
        List<SignProject> list = projectMapper.selectProjectListByUserId(userId);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .filter(p -> SignProject.STATUS_IN_PROGRESS.equals(p.getStatus()))
                .toList();
    }

    @Override
    public boolean prepareSign(SignRequest request) {
        request.setSignMode(requestNormalizer.normalizeMode(request.getSignMode()));
        validateSingleModeOnly(request);
        validateExecutableRequest(request, UserContextHolder.getUserId());
        return true;
    }

    @Override
    public SignVerifyReceipt selectReceiptByRecordId(String recordId) {
        // 【越权校验 2026-08-09】仅管理员/签章本人/原始文件Owner 允许查看或下载该签章回单
        Long userId = UserContextHolder.getUserId();
        assertCanAccessReceipt(recordId, userId);
        return receiptService.selectByRecordId(recordId);
    }

    /**
     * 访问权限（签章记录 & 回单）：管理员 ｜ 签章执行人 ｜ 原始文件 owner。
     * 项目成员如果既不是 owner 也没签，则不允许随便看到别人的证书指纹、业务信息。
     */
    private void assertCanAccessReceipt(String recordId, Long userId) {
        if (StringUtils.isEmpty(recordId)) {
            throw new ServiceException("参数错误：recordId 为空");
        }
        if (isAdminUser(userId)) {
            return;
        }
        SignRecord rec = recordMapper.selectRecordById(recordId);
        if (rec == null) {
            throw new ServiceException("签章记录不存在");
        }
        // 签章本人
        if (userId != null && userId.equals(rec.getUserId())) {
            return;
        }
        // 原始文件所有者
        if (rec.getFileId() != null) {
            SignFile origin = fileMapper.selectFileById(rec.getFileId());
            if (origin != null && userId != null && userId.equals(origin.getOwnerId())) {
                return;
            }
        }
        // 已签章输出文件的所有者（很少会变，但一起兼容）
        if (rec.getSignedFileId() != null) {
            SignFile signed = fileMapper.selectFileById(rec.getSignedFileId());
            if (signed != null && userId != null && userId.equals(signed.getOwnerId())) {
                return;
            }
        }
        throw new ServiceException("越权访问：您无权查看该签章记录的回单");
    }

    /**
     * 检查用户是否有印章使用权限
     */
    private boolean checkSealPermission(Long sealId, Long userId) {
        if (isAdminUser(userId)) {
            return true; // 管理员拥有所有权限
        }
        return permissionMapper.existsPermission(sealId, userId) > 0;
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

    /**
     * 校验签章请求是否可执行，同时返回加载好的印章对象（避免重复查询）
     * @return 印章对象，如果是纯时间戳模式则返回 null
     */
    private SignSeal validateExecutableRequest(SignRequest request, Long userId) {
        return validateExecutableRequest(request, userId, false);
    }

    private SignSeal validateExecutableRequest(SignRequest request, Long userId, boolean lockSeal) {
        // 按开始/结束时间即时刷新，避免已到开始时间仍停留在「未开始」无法签章
        projectMapper.refreshExpiredProjects();
        SignProject project = projectMapper.selectProjectById(request.getProjectId());
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!SignProject.STATUS_IN_PROGRESS.equals(project.getStatus())) {
            throw new ServiceException("项目状态不是进行中，无法进行签章");
        }
        validateSinglePlacement(request);
        if (requestNormalizer.isTimestampOnlyRequest(request)) {
            return null;
        }
        Long sealId = request.getSealId();
        if (sealId == null) {
            throw new ServiceException("印章不存在");
        }
        SignSeal seal = lockSeal ? sealMapper.selectSealByIdForUpdate(sealId) : sealMapper.selectSealById(sealId);
        if (seal == null || !"0".equals(seal.getDelFlag()) || !"0".equals(seal.getStatus())) {
            throw new ServiceException("印章不可用");
        }
        validateSealInProject(request.getProjectId(), sealId);
        if (!checkSealPermission(sealId, userId)) {
            throw new ServiceException("您没有该印章的使用权限");
        }
        if (shouldUseManagedKey(request, seal)) {
            assertManagedSealReady(seal);
            // P0-4: 托管证书必须校验有效期与吊销状态，防止用过期/已吊销证书签章
            assertManagedCertValid(seal);
        }
        return seal;
    }

    private void validateSingleModeOnly(SignRequest request) {
        String mode = request.getSignMode();
        if (!"single".equals(mode) && !"fixed".equals(mode) && !"custom".equals(mode) && !"span".equals(mode)) {
            throw new ServiceException("不支持的签章模式: " + mode);
        }
    }

    private void validateSinglePlacement(SignRequest request) {
        String mode = request.getSignMode();
        if (request.getPositions() == null || request.getPositions().isEmpty()) {
            throw new ServiceException("签章位置不能为空");
        }
        // fixed 模式：前端已展开为所有页位置，或传入单个位置由后端展开
        // span 模式：前端传入单个位置，后端展开为切片
        // single 和 custom 模式：支持多个位置（多印章共存）
        for (SignPosition position : request.getPositions()) {
            if (position == null || position.getPage() == null || position.getX() == null || position.getY() == null) {
                throw new ServiceException("签章位置不完整");
            }
            if (position.getWidth() == null || position.getHeight() == null) {
                throw new ServiceException("签章矩形尺寸不完整");
            }
        }
    }

    private void validateSealInProject(Long projectId, Long sealId) {
        if (projectId == null || sealId == null) {
            return;
        }
        int configuredSealCount = projectMapper.countProjectSeals(projectId);
        if (configuredSealCount > 0 && projectMapper.existsProjectSeal(projectId, sealId) <= 0) {
            throw new ServiceException("当前项目未配置该印章");
        }
    }

    private SignSeal ensureManagedSealReady(SignSeal seal) {
        if (seal == null || seal.getSealId() == null) {
            throw new ServiceException("印章未绑定平台证书");
        }
        SignSeal refreshedSeal = seal;
        if (sealService != null) {
            sealService.bindSealCert(seal.getSealId());
            SignSeal dbSeal = sealMapper.selectSealById(seal.getSealId());
            if (dbSeal != null) {
                refreshedSeal = dbSeal;
            }
        }
        SignCert cert = signCertMapper.selectActiveBySealId(seal.getSealId());
        if (cert == null && refreshedSeal.getCertId() != null) {
            cert = signCertMapper.selectSignCertByCertId(refreshedSeal.getCertId());
        }
        String keyRef = cert != null && StringUtils.isNotEmpty(cert.getKeyRef())
                ? cert.getKeyRef()
                : refreshedSeal.getKeyRef();
        Long certId = cert != null && cert.getCertId() != null
                ? cert.getCertId()
                : refreshedSeal.getCertId();
        if (certId == null || StringUtils.isEmpty(keyRef)) {
            throw new ServiceException("印章未绑定平台证书");
        }
        seal.setCertId(certId);
        seal.setKeyRef(keyRef);
        seal.setCertBindMode(StringUtils.isNotEmpty(refreshedSeal.getCertBindMode()) ? refreshedSeal.getCertBindMode() : "managed");
        return seal;
    }

    void assertManagedSealReady(SignSeal seal) {
        ensureManagedSealReady(seal);
    }

    /**
     * P0-4: 校验托管证书有效期与吊销状态
     * <p>禁止使用过期、未生效或已吊销的证书执行签章，否则产出的签名无法律效力。</p>
     */
    private void assertManagedCertValid(SignSeal seal) {
        SignCert cert = signCertMapper.selectActiveBySealId(seal.getSealId());
        if (cert == null && seal.getCertId() != null) {
            cert = signCertMapper.selectSignCertByCertId(seal.getCertId());
        }
        if (cert == null) {
            throw new ServiceException("证书不存在或已吊销");
        }
        Date now = new Date();
        if (cert.getNotBefore() != null && now.before(cert.getNotBefore())) {
            throw new ServiceException("证书尚未生效（生效时间: " + cert.getNotBefore() + "）");
        }
        if (cert.getNotAfter() != null && now.after(cert.getNotAfter())) {
            throw new ServiceException("证书已过期（过期时间: " + cert.getNotAfter() + "）");
        }
        if ("2".equals(cert.getStatus())) {
            throw new ServiceException("证书已吊销，禁止用于签章");
        }
    }

    void applyManagedCertSnapshot(SignRecord record, SignSeal seal, Date signTime) {
        assertManagedSealReady(seal);
        SignCert cert = signCertMapper.selectSignCertByCertId(seal.getCertId());
        if (cert == null) {
            throw new ServiceException("平台证书不存在或已失效");
        }
        record.setCertId(cert.getCertId());
        record.setSerialNo(cert.getSerialNo());
        record.setFingerprintSha256(cert.getFingerprintSha256());
        record.setVerifyStatus("1");
        // P0-6: sign_time 不再由 Java 设置，改由数据库 CURRENT_TIMESTAMP(3) 自动填充
    }

    void createReceiptSnapshot(SignRecord record) {
        if (record == null || StringUtils.isEmpty(record.getRecordId())) {
            return;
        }
        SignVerifyReceipt receipt = receiptService.buildReceipt(
                record.getRecordId(),
                record.getFileId(),
                record.getSignedFileId(),
                record.getCertId(),
                record.getSerialNo(),
                record.getFingerprintSha256());
        signVerifyReceiptMapper.insertSignVerifyReceipt(receipt);
    }

    void persistSnapshotHashes(SignRecord record) {
        if (record == null || snapshotFingerprintHelper == null) {
            return;
        }

        String pageFingerprint = snapshotFingerprintHelper.buildPageFingerprint(
                record.getSealId(),
                record.getSignMode(),
                record.getPositions());
        if (StringUtils.isEmpty(pageFingerprint)) {
            log.warn("Skip snapshot hash persistence because page fingerprint cannot be generated, recordId={}",
                    record.getRecordId());
            return;
        }

        record.setPageFingerprint(pageFingerprint);
        String signHash = snapshotFingerprintHelper.buildSignHash(record);
        if (StringUtils.isEmpty(signHash)) {
            record.setPageFingerprint(null);
            log.warn("Skip snapshot hash persistence because sign hash cannot be generated, recordId={}",
                    record.getRecordId());
            return;
        }

        record.setSignHash(signHash);
        recordMapper.updateRecord(record);
    }

    /**
     * 判断是否使用托管证书签名
     * <p>
     * 注意：骑缝章（span）模式暂不支持托管证书签名，原因如下：
     * <ul>
     *   <li>骑缝章是将一个印章切片后分布在多页边缘，可视区域是多个小切片</li>
     *   <li>PDF数字签名需要明确的签名字段（AcroForm），一个签名只能对应一个矩形区域</li>
     *   <li>如要实现骑缝章数字签名，需研究：每一页加一个独立签名 / 或使用一个大签名区域覆盖多页切片</li>
     * </ul>
     * 业务现状：骑缝章仅做印章图片渲染，不具备 CA 数字签名的法律效力。
     * 如需法律效力，请使用单页签章（single）或逐页签章（fixed）模式。
     *
     * @param request 签章请求
     * @param seal 印章对象
     * @return true-使用托管证书，false-不使用
     */
    private boolean shouldUseManagedKey(SignRequest request, SignSeal seal) {
        return request != null
                && "1".equals(request.getUseManagedKey())
                && seal != null
                && !"span".equalsIgnoreCase(request.getSignMode());
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA256计算失败", e);
        }
    }

    private String getSignModeName(String signMode) {
        if (signMode == null) return "签章";
        switch (signMode.toLowerCase()) {
            case "single":
                return "单页签章";
            case "fixed":
            case "multi":
            case "continuous":
                return "逐页签章";
            case "custom":
                return "定点签章";
            case "span":
                return "骑缝签章";
            default:
                return "签章";
        }
    }

    private int countPdfPages(byte[] bytes) {
        try (PdfDocument document = new PdfDocument(new PdfReader(new java.io.ByteArrayInputStream(bytes)))) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            throw new ServiceException("无法解析 PDF 页数: " + e.getMessage());
        }
    }
}
