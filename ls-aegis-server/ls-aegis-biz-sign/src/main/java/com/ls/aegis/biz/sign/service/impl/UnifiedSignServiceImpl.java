package com.ls.aegis.biz.sign.service.impl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.compat.common.utils.ServletUtils;
import com.ls.aegis.compat.common.utils.http.UserAgentUtils;
import com.ls.aegis.compat.common.utils.ip.AddressUtils;
import com.ls.aegis.compat.common.utils.ip.IpUtils;
import com.ls.aegis.biz.sign.chain.SignChainContext;
import com.ls.aegis.biz.sign.chain.SignTask;
import com.ls.aegis.biz.sign.chain.TaskChainExecutor;
import com.ls.aegis.biz.sign.chain.task.DigitalSignTask;
import com.ls.aegis.biz.sign.chain.task.GeneralSealRenderTask;
import com.ls.aegis.biz.sign.chain.task.SealImageLoadTask;
import com.ls.aegis.biz.sign.chain.task.SealImageProcessTask;
import com.ls.aegis.biz.sign.chain.task.SpanSealRenderTask;
import com.ls.aegis.biz.sign.domain.SignCert;
import com.ls.aegis.biz.sign.domain.SignFile;
import com.ls.aegis.biz.sign.domain.SignLog;
import com.ls.aegis.biz.sign.domain.SignProject;
import com.ls.aegis.biz.sign.domain.SignRecord;
import com.ls.aegis.biz.sign.domain.SignSeal;
import com.ls.aegis.biz.sign.domain.SignVerifyReceipt;
import com.ls.aegis.biz.sign.domain.vo.SignResult;
import com.ls.aegis.biz.sign.domain.vo.UnifiedSignRequest;
import com.ls.aegis.biz.sign.mapper.SignCertMapper;
import com.ls.aegis.biz.sign.mapper.SignFileMapper;
import com.ls.aegis.biz.sign.mapper.SignLogMapper;
import com.ls.aegis.biz.sign.mapper.SignProjectMapper;
import com.ls.aegis.biz.sign.mapper.SignRecordMapper;
import com.ls.aegis.biz.sign.mapper.SignSealMapper;
import com.ls.aegis.biz.sign.mapper.SignVerifyReceiptMapper;
import com.ls.aegis.biz.sign.service.IUnifiedSignService;
import com.ls.aegis.biz.sign.service.IReceiptService;
import com.ls.aegis.biz.sign.utils.SignDataScopeHelper;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class UnifiedSignServiceImpl implements IUnifiedSignService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedSignServiceImpl.class);

    @Autowired
    private TaskChainExecutor taskChainExecutor;

    @Autowired
    private SealImageLoadTask sealImageLoadTask;

    @Autowired
    private SealImageProcessTask sealImageProcessTask;

    @Autowired
    private GeneralSealRenderTask generalSealRenderTask;

    @Autowired
    private SpanSealRenderTask spanSealRenderTask;

    @Autowired
    private DigitalSignTask digitalSignTask;

    @Autowired
    private SignFileMapper fileMapper;

    @Autowired
    private SignSealMapper sealMapper;

    @Autowired
    private SignCertMapper signCertMapper;

    @Autowired
    private SignRecordMapper recordMapper;

    @Autowired
    private SignLogMapper logMapper;

    @Autowired
    private SignProjectMapper projectMapper;

    @Autowired
    private SignFileServiceImpl fileService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private com.ls.aegis.biz.sign.service.impl.SignSnapshotFingerprintHelper snapshotFingerprintHelper;

    @Autowired(required = false)
    private IReceiptService receiptService;

    @Autowired(required = false)
    private SignVerifyReceiptMapper signVerifyReceiptMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SignResult executeUnifiedSign(UnifiedSignRequest request) {
        Long userId = UserContextHolder.getUserId();

        log.info("合并签章开始: fileId={}, sysFileId={}, generalSeals={}, spanSeals={}",
            request.getFileId(), request.getSysFileId(),
            request.getGeneralSeals() != null ? request.getGeneralSeals().size() : 0,
            request.getSpanSeals() != null ? request.getSpanSeals().size() : 0);

        assertProjectInProgress(request.getProjectId());

        request.setFileId(fileService.resolveToSignFileId(request.getFileId(), request.getSysFileId()));
        if (request.getFileId() == null) {
            throw new ServiceException("文件ID不能为空");
        }

        SignFile sourceFile = fileMapper.selectFileById(request.getFileId());
        if (sourceFile == null) {
            throw new ServiceException("源文件不存在");
        }

        byte[] sourcePdf;
        try {
            sourcePdf = fileService.getFileBytes(request.getFileId());
        } catch (Exception e) {
            throw new ServiceException("无法读取源文件内容: " + e.getMessage());
        }
        if (sourcePdf == null || sourcePdf.length == 0) {
            throw new ServiceException("源文件内容为空");
        }

        int pageCount;
        float pageWidth;
        float pageHeight;
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(sourcePdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            pageCount = pdfDoc.getNumberOfPages();
            pageWidth = pdfDoc.getFirstPage().getPageSize().getWidth();
            pageHeight = pdfDoc.getFirstPage().getPageSize().getHeight();
        } catch (Exception e) {
            throw new ServiceException("PDF解析失败: " + e.getMessage());
        }

        SignChainContext context = new SignChainContext();
        context.setSourcePdf(sourcePdf);
        context.setPageCount(pageCount);
        context.setPageWidth(pageWidth);
        context.setPageHeight(pageHeight);

        if (request.getGeneralSeals() != null) {
            context.setGeneralSeals(request.getGeneralSeals());
        }
        if (request.getSpanSeals() != null) {
            context.setSpanSeals(request.getSpanSeals());
        }

        Long primarySealId = request.getSealId();
        if (primarySealId == null && request.getGeneralSeals() != null && !request.getGeneralSeals().isEmpty()) {
            primarySealId = request.getGeneralSeals().get(0).getSealId();
        }
        if (primarySealId == null && request.getSpanSeals() != null && !request.getSpanSeals().isEmpty()) {
            primarySealId = request.getSpanSeals().get(0).getSealId();
        }
        context.setPrimarySealId(primarySealId);
        context.setSkipSealId(primarySealId);

        List<SignTask> tasks = buildTaskChain();
        taskChainExecutor.executeSequential(tasks, context);

        byte[] finalPdf = context.getFinalPdf() != null ? context.getFinalPdf()
            : (context.getRenderedPdf() != null ? context.getRenderedPdf() : sourcePdf);

        SignResult result = saveAndRecord(finalPdf, sourceFile, request, primarySealId, userId);

        log.info("合并签章完成: recordId={}", result.getRecordId());
        return result;
    }

    private List<SignTask> buildTaskChain() {
        List<SignTask> tasks = new ArrayList<>();

        tasks.add(sealImageLoadTask);
        tasks.add(sealImageProcessTask);
        tasks.add(spanSealRenderTask);
        tasks.add(generalSealRenderTask);
        tasks.add(digitalSignTask);

        return tasks;
    }

    /** 与 /prepare、/run 一致：仅进行中项目可合并签章 */
    private void assertProjectInProgress(Long projectId) {
        if (projectId == null) {
            throw new ServiceException("签章项目不能为空");
        }
        projectMapper.refreshExpiredProjects();
        SignProject project = projectMapper.selectProjectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!SignProject.STATUS_IN_PROGRESS.equals(project.getStatus())) {
            throw new ServiceException("项目状态不是进行中，无法进行签章");
        }
    }

    private SignResult saveAndRecord(byte[] signedPdf, SignFile originalFile,
                                      UnifiedSignRequest request, Long primarySealId, Long userId) {
        Date signTime = new Date();
        String signTimeStr = new SimpleDateFormat("yyyyMMddHHmmss").format(signTime);
        String originalName = originalFile.getFileName();
        int dotIdx = originalName.lastIndexOf('.');
        String baseName = dotIdx > 0 ? originalName.substring(0, dotIdx) : originalName;
        String ext = dotIdx > 0 ? originalName.substring(dotIdx) : ".pdf";
        String displayFileName = baseName + "_合并签章_" + signTimeStr + ext;

        SignFile signedFile = fileService.uploadBytes(signedPdf, displayFileName, "sign");

        String recordId = UUID.randomUUID().toString().replace("-", "");

        SignRecord record = new SignRecord();
        record.setRecordId(recordId);
        record.setFileId(request.getFileId());
        record.setSealId(primarySealId);
        record.setUserId(userId);
        record.setProjectId(request.getProjectId());
        record.setSignMode("unified");
        try {
            java.util.Map<String, Object> posMap = new java.util.HashMap<>();
            if (request.getGeneralSeals() != null) {
                posMap.put("generalSeals", request.getGeneralSeals());
            }
            if (request.getSpanSeals() != null) {
                posMap.put("spanSeals", request.getSpanSeals());
            }
            record.setPositions(objectMapper.writeValueAsString(posMap));
        } catch (Exception e) {
            record.setPositions("{}");
        }
        record.setStatus("completed");
        record.setSignedFileId(signedFile.getFileId());
        record.setCreateTime(XmlMapperDO.from(signTime));
        record.setSignTime(signTime);
        // 客户端信息
        String clientIp = IpUtils.getIpAddr();
        record.setClientIp(clientIp);
        record.setIpLocation(AddressUtils.getRealAddressByIP(clientIp));
        // 从请求头获取 User-Agent 解析浏览器和操作系统
        String userAgent = "";
        try {
            userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            // 非 Web 上下文，忽略
        }
        record.setOsVersion(UserAgentUtils.getOperatingSystem(userAgent));
        record.setBrowserVersion(UserAgentUtils.getBrowser(userAgent));
        // 时间戳：合并签章默认不添加
        record.setHasTimestamp("0");
        // 回单证书信息：从主印章绑定的托管证书快照写入（与 /run 托管签章路径对齐）
        applyManagedCertSnapshot(record, primarySealId);
        SignProject project = request.getProjectId() != null
            ? projectMapper.selectProjectById(request.getProjectId()) : null;
        SignDataScopeHelper.fillAuditRow(record, userId, project);
        recordMapper.insertRecord(record);
        persistSnapshotHashes(record);
        createReceiptSnapshot(record);

        SignLog signLog = new SignLog();
        signLog.setProjectId(request.getProjectId());
        signLog.setSealId(primarySealId);
        signLog.setUserId(userId);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(signedPdf);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            signLog.setFileHash(sb.toString());
        } catch (Exception e) {
            signLog.setFileHash("-");
        }
        signLog.setClientIp(IpUtils.getIpAddr());
        signLog.setIpLocation(AddressUtils.getRealAddressByIP(IpUtils.getIpAddr()));
        signLog.setActionType("SIGN_ADD");
        signLog.setResultCode("SUCCESS");
        signLog.setOutputFileId(signedFile.getFileId());
        signLog.setSignTime(signTime);
        signLog.setCreateTime(XmlMapperDO.from(signTime));
        signLog.setRemark("合并签章");
        SignDataScopeHelper.fillAuditRow(signLog, userId, project);
        logMapper.insertLog(signLog);

        SignResult result = new SignResult();
        result.setRecordId(recordId);
        result.setSignedFileId(signedFile.getFileId());
        result.setSignedFileName(displayFileName);
        result.setSignTime(signTime);

        return result;
    }

    /**
     * 将主印章托管证书写入签章记录，供验签回单展示证书ID/序列号/指纹。
     * 无托管证书时保持为空（仅图片盖章场景）。
     */
    void applyManagedCertSnapshot(SignRecord record, Long primarySealId) {
        if (record == null || primarySealId == null || signCertMapper == null || sealMapper == null) {
            return;
        }
        SignSeal seal = sealMapper.selectSealById(primarySealId);
        if (seal == null) {
            return;
        }
        SignCert cert = signCertMapper.selectActiveBySealId(primarySealId);
        if (cert == null && seal.getCertId() != null) {
            cert = signCertMapper.selectSignCertByCertId(seal.getCertId());
        }
        if (cert == null) {
            return;
        }
        record.setCertId(cert.getCertId());
        record.setSerialNo(cert.getSerialNo());
        record.setFingerprintSha256(cert.getFingerprintSha256());
        record.setVerifyStatus("1");
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

    void createReceiptSnapshot(SignRecord record) {
        if (record == null || StringUtils.isEmpty(record.getRecordId()) || receiptService == null
                || signVerifyReceiptMapper == null) {
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
}
