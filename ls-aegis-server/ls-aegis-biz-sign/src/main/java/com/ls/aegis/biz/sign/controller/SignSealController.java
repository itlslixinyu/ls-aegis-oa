package com.ls.aegis.biz.sign.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.FileMagicValidator;
import com.ls.aegis.compat.common.utils.PasswordEncryptUtils;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.sign.domain.SignSeal;
import com.ls.aegis.biz.sign.model.resp.SignSealExportResp;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.biz.sign.domain.SignSealExtract;
import com.ls.aegis.biz.sign.mapper.SignSealExtractMapper;
import com.ls.aegis.biz.sign.mapper.SignSealMapper;
import com.ls.aegis.biz.sign.service.ISealExtractService;
import com.ls.aegis.biz.sign.service.ISignStorageService;
import com.ls.aegis.biz.sign.service.ISignSealService;
import com.ls.aegis.biz.sign.util.SealImageProcessor;
import com.ls.aegis.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 印章管理
 * 【安全强制】类级别要求登录态；防止图片代理、提取记录等接口漏加权限时被匿名调用。
 */
@RestController
@RequestMapping("/sign/seal")
@SaCheckLogin
public class SignSealController {

    private static final Logger log = LoggerFactory.getLogger(SignSealController.class);

    @Autowired
    private ISignSealService sealService;

    @Autowired
    private ISealExtractService extractService;

    @Autowired
    private SignSealMapper signSealMapper;

    @Autowired
    private SignSealExtractMapper extractMapper;

    @Autowired
    private ISignStorageService signStorage;

    @Autowired
    private com.ls.aegis.biz.sign.service.impl.CertService certService;

    /** 图像处理工具类实例（委托给 Service 层） */
    private final SealImageProcessor imageProcessor = new SealImageProcessor();

    @SaCheckPermission("sign:seal:list")
    @GetMapping("/list")
    public R<PageResp<SignSeal>> list(SignSeal seal, @Valid PageQuery pageQuery) {
        return R.ok(PageQueryHelper.page(pageQuery, () -> sealService.selectSealList(seal)));
    }

    @Log(module = "印章管理-导出")
    @SaCheckPermission("sign:seal:export")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(HttpServletResponse response, SignSeal seal) {
        PageQueryHelper.startExportLimit();
        List<SignSeal> list = sealService.selectSealList(seal);
        ExcelExportHelper.export(list, SignSealExportResp.class, "印章数据", response);
    }

    @SaCheckPermission("sign:seal:query")
    @GetMapping("/{sealId}")
    public R<SignSeal> getInfo(@PathVariable Long sealId) {
        return R.ok(sealService.selectSealById(sealId));
    }

    @SaCheckPermission("sign:seal:edit")
    @PostMapping("/bind-cert")
    public R<?> bindCert(@RequestBody(required = false) Map<String, Object> body) {
        Long sealId = parseSealId(body == null ? null : body.get("sealId"));
        if (sealId == null) {
            throw new ServiceException("缺少印章ID");
        }
        return R.ok(sealService.bindSealCert(sealId));
    }

    /** 前端雪花 ID 常以字符串传输，兼容 Number / String */
    private Long parseSealId(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @SaCheckPermission("sign:seal:query")
    @GetMapping("/cert-info/{sealId}")
    public R<?> certInfo(@PathVariable Long sealId) {
        return R.ok(sealService.getSealCertInfo(sealId));
    }

    /**
     * 自动生成下一个印章编号（YZ + 3位递增）
     */
    @SaCheckPermission("sign:seal:add")
    @GetMapping("/code/next")
    public R<String> nextSealCode() {
        try {
            String nextCode;
            try {
                nextCode = signSealMapper.selectNextSealCodeWithSequence();
            } catch (Exception e) {
                nextCode = signSealMapper.selectNextSealCode();
            }
            return R.ok(nextCode);
        } catch (Exception e) {
            throw new ServiceException("生成编号失败：" + e.getMessage());
        }
    }

    /**
     * 创建印章（含图片上传：1. 直接上传 file；2. 引用已存在的提取记录 extractRecordId）
     */
    @Log(module = "印章管理-新增")
    @SaCheckPermission("sign:seal:add")
    @PostMapping
    public R<Void> add(@RequestPart(value = "file", required = false) MultipartFile file,
        @RequestParam("sealName") String sealName,
        @RequestParam(value = "sealCode", required = false) String sealCode,
        @RequestParam("sealCategory") String sealCategory,
        @RequestParam(value = "sealType", required = false) String sealType,
        @RequestParam(value = "enableDate", required = false) String enableDate,
        @RequestParam(value = "deptId", required = false) Long deptId,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "extractRecordId", required = false) Long extractRecordId,
        @RequestParam(value = "remark", required = false) String remark,
        @RequestParam(value = "certFile", required = false) MultipartFile certFile,
        @RequestParam(value = "certPassword", required = false) String certPassword) {
        String sealFilePath = null;
        if (file != null && !file.isEmpty()) {
            FileMagicValidator.assertSealImage(file);
            try {
                sealFilePath = signStorage.upload(file.getBytes(), file.getOriginalFilename(), "seal");
            } catch (Exception e) {
                throw new ServiceException("印章图片上传失败：" + e.getMessage());
            }
        } else if (extractRecordId != null) {
            SignSealExtract record = extractMapper.selectExtractById(extractRecordId);
            if (record == null || record.getExtractFilePath() == null) {
                throw new ServiceException("提取记录不存在或无图片");
            }
            sealFilePath = record.getExtractFilePath();
        } else {
            throw new ServiceException("请上传印章图片或从提取记录选择");
        }

        SignSeal seal = new SignSeal();
        seal.setSealName(sealName);
        seal.setSealCode(sealCode);
        seal.setSealCategory(sealCategory);
        seal.setSealType(sealType);
        seal.setEnableDate(enableDate);
        seal.setDeptId(deptId);
        seal.setStatus(status != null ? status : "0");
        seal.setRemark(remark);
        seal.setSealFilePath(sealFilePath);

        processCertFile(seal, certFile, certPassword);
        sealService.insertSeal(seal);
        return R.ok();
    }

    /**
     * 修改印章：支持纯 JSON 编辑，也支持 multipart 文件上传
     */
    @Log(module = "印章管理-修改")
    @SaCheckPermission("sign:seal:edit")
    @PutMapping
    public R<Void> editJson(@RequestBody(required = false) SignSeal seal,
        @RequestPart(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "sealId", required = false) Long sealId,
        @RequestParam(value = "sealName", required = false) String sealName,
        @RequestParam(value = "sealCode", required = false) String sealCode,
        @RequestParam(value = "sealCategory", required = false) String sealCategory,
        @RequestParam(value = "sealType", required = false) String sealType,
        @RequestParam(value = "enableDate", required = false) String enableDate,
        @RequestParam(value = "deptId", required = false) Long deptId,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "remark", required = false) String remark,
        @RequestParam(value = "certFile", required = false) MultipartFile certFile,
        @RequestParam(value = "certPassword", required = false) String certPassword) {
        if (seal != null && seal.getSealId() != null) {
            sealService.updateSeal(seal);
            return R.ok();
        }
        if (sealId != null) {
            doUpdateSeal(file, sealId, sealName, sealCode, sealCategory, sealType,
                enableDate, deptId, status, remark, certFile, certPassword);
            return R.ok();
        }
        throw new ServiceException("参数错误：sealId 不能为空");
    }

    private void doUpdateSeal(MultipartFile file, Long sealId, String sealName,
        String sealCode, String sealCategory, String sealType,
        String enableDate, Long deptId, String status, String remark,
        MultipartFile certFile, String certPassword) {
        SignSeal existing = signSealMapper.selectSealById(sealId);
        if (existing == null) {
            throw new ServiceException("印章不存在");
        }

        SignSeal seal = new SignSeal();
        seal.setSealId(sealId);
        seal.setSealName(sealName);
        seal.setSealCode(sealCode);
        seal.setSealCategory(sealCategory);
        seal.setSealType(sealType);
        seal.setEnableDate(enableDate);
        seal.setDeptId(deptId);
        seal.setStatus(status != null ? status : "0");
        seal.setRemark(remark);
        seal.setSealFilePath(existing.getSealFilePath());

        if (file != null && !file.isEmpty()) {
            FileMagicValidator.assertSealImage(file);
            try {
                String newPath = signStorage.upload(file.getBytes(), file.getOriginalFilename(), "seal");
                seal.setSealFilePath(newPath);
            } catch (Exception e) {
                throw new ServiceException("印章图片上传失败：" + e.getMessage());
            }
        }

        if (certFile != null && !certFile.isEmpty()) {
            processCertFile(seal, certFile, certPassword);
        } else {
            seal.setCertPath(existing.getCertPath());
            seal.setCertInfo(existing.getCertInfo());
        }

        sealService.updateSeal(seal);
    }

    /**
     * 处理证书文件：上传到 MinIO 并解析证书信息
     */
    private void processCertFile(SignSeal seal, MultipartFile certFile, String certPassword) {
        if (certFile == null || certFile.isEmpty()) {
            return;
        }
        long maxCertSize = 4L * 1024 * 1024;
        if (certFile.getSize() > maxCertSize) {
            throw new ServiceException(
                "证书文件过大（" + certFile.getSize() + " 字节），上限 " + (maxCertSize / 1024 / 1024) + "MB");
        }
        if (StringUtils.isEmpty(certPassword)) {
            throw new ServiceException("证书密码不能为空（PKCS#12 文件必须有密码保护）");
        }
        FileMagicValidator.assertPkcs12(certFile);
        try {
            String certPath = signStorage.upload(certFile.getBytes(),
                certFile.getOriginalFilename(), "seal-cert");
            seal.setCertPath(certPath);

            if (StringUtils.isNotEmpty(certPassword)) {
                seal.setCertPassword(PasswordEncryptUtils.encrypt(certPassword));
            }

            try {
                java.security.KeyStore keyStore = certService.loadFromPfxBytes(
                    certFile.getBytes(), certPassword);
                java.security.cert.Certificate[] chain = certService.getCertificateChain(keyStore);
                if (chain != null && chain.length > 0) {
                    String certInfo = certService.parseCertInfo(chain[0]);
                    seal.setCertInfo(certInfo);
                }
            } catch (Exception ex) {
                log.warn("解析证书信息失败: {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("证书上传失败: {}", e.getMessage());
        }
    }

    /**
     * 后端代理：预览印章图片
     */
    @SaCheckPermission("sign:seal:query")
    @GetMapping("/image/{sealId}")
    public void getSealImage(@PathVariable Long sealId, HttpServletResponse response) {
        try {
            Long userId = UserContextHolder.getUserId();
            if (!sealService.canAccessSealImage(sealId, userId)) {
                response.setStatus(403);
                return;
            }
            SignSeal seal = signSealMapper.selectSealById(sealId);
            if (seal == null || seal.getSealFilePath() == null) {
                response.setStatus(404);
                return;
            }
            byte[] data = signStorage.download(seal.getSealFilePath());
            response.setContentType("image/png");
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("印章图片代理失败", e);
            response.setStatus(500);
        }
    }

    @Log(module = "印章管理-删除")
    @SaCheckPermission("sign:seal:remove")
    @DeleteMapping("/{sealIds}")
    public R<Void> remove(@PathVariable Long[] sealIds) {
        sealService.deleteSealByIds(sealIds);
        return R.ok();
    }

    /**
     * 获取当前用户可用印章列表
     */
    @SaCheckPermission("sign:seal:use")
    @GetMapping("/available")
    public R<List<SignSeal>> available(@RequestParam(required = false) Long projectId) {
        Long userId = UserContextHolder.getUserId();
        List<SignSeal> seals = sealService.selectAvailableSealList(userId, projectId);
        for (SignSeal seal : seals) {
            if (seal.getSealId() != null) {
                seal.setSealFilePath("/sign/seal/image/" + seal.getSealId());
            }
        }
        return R.ok(seals);
    }

    /**
     * 印章提取 - 仅预览，不保存
     */
    @Log(module = "印章提取-预览")
    @SaCheckPermission("sign:seal:extract")
    @PostMapping("/extract/preview")
    public R<Map<String, Object>> extractPreview(@RequestParam("file") MultipartFile file,
        @RequestParam(value = "sealCategory", required = false) String sealCategory) {
        FileMagicValidator.assertSealImage(file);
        try {
            String originalName = file.getOriginalFilename();
            Map<String, Object> result = extractService.extractSealCircle(
                file.getBytes(),
                UserContextHolder.getUserId(),
                UserContextHolder.getUsername(),
                sealCategory
            );
            result.put("originalFileName", originalName);
            result.put("originalFileSize", file.getSize());
            return R.ok(result);
        } catch (Exception e) {
            throw new ServiceException("印章提取失败：" + e.getMessage());
        }
    }

    /**
     * 印章提取 - 保存
     */
    @Log(module = "印章提取-保存")
    @SaCheckPermission("sign:seal:extract")
    @PostMapping("/extract/save")
    public R<Map<String, Object>> extractSave(@RequestParam("file") MultipartFile file,
        @RequestParam("fileName") String fileName,
        @RequestParam(value = "sealCategory", required = false) String sealCategory) {
        FileMagicValidator.assertSealImage(file);
        try {
            String originalName = file.getOriginalFilename();

            String originalObjectName = signStorage.upload(file.getBytes(), originalName, "seal-extract-original");

            Map<String, Object> extractResult = extractService.extractSealCircle(
                file.getBytes(),
                UserContextHolder.getUserId(),
                UserContextHolder.getUsername(),
                sealCategory
            );
            byte[] extractBytes = (byte[]) extractResult.get("extractBytes");
            Integer targetSizeMm = (Integer) extractResult.get("targetSizeMm");

            String displayName = fileName;
            if (displayName == null || displayName.isEmpty()) {
                displayName = "extracted-seal.png";
            }
            displayName = displayName.trim();
            if (!displayName.toLowerCase().endsWith(".png")) {
                displayName = displayName + ".png";
            }

            String objectName = signStorage.upload(extractBytes, displayName, "seal-extract");

            byte[] thumbnailBytes = imageProcessor.generateThumbnail(extractBytes, 100);
            String thumbnailObjectName = signStorage.upload(thumbnailBytes,
                "thumb-" + System.currentTimeMillis() + ".png", "seal-extract-thumb");

            SignSealExtract record = new SignSealExtract();
            record.setOriginalFileName(originalName);
            record.setOriginalFileSize(file.getSize());
            record.setRemark(originalObjectName);
            record.setExtractFileName(displayName);
            record.setExtractFilePath(objectName);
            record.setExtractFileSize((long) extractBytes.length);
            record.setThumbnailFilePath(thumbnailObjectName);
            if (targetSizeMm != null) {
                record.setDefaultSizeMm(targetSizeMm);
            }

            record.setCreateUserId(UserContextHolder.getUserId());
            record.setCreateBy(UserContextHolder.getUsername());
            extractMapper.insertExtract(record);

            Map<String, Object> result = new HashMap<>();
            result.put("recordId", record.getId());
            result.put("originalFileName", originalName);
            result.put("extractFileName", displayName);
            result.put("extractImageUrl", "/sign/seal/extract/image/" + record.getId());
            result.put("originalImageUrl", "/sign/seal/extract/image/original/" + record.getId());
            result.put("targetSizeMm", targetSizeMm);

            return R.ok(result);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("保存失败：" + e.getMessage());
        }
    }

    @SaCheckPermission("sign:seal:extract")
    @GetMapping("/extract/image/{recordId}")
    public void getExtractImage(@PathVariable Long recordId,
        HttpServletRequest request,
        HttpServletResponse response) {
        proxyImageDownload(recordId, false, false, false, request, response);
    }

    @SaCheckPermission("sign:seal:extract")
    @GetMapping("/extract/image/thumb/{recordId}")
    public void getExtractThumbnail(@PathVariable Long recordId,
        HttpServletRequest request,
        HttpServletResponse response) {
        proxyImageDownload(recordId, false, false, true, request, response);
    }

    @SaCheckPermission("sign:seal:extract")
    @GetMapping("/extract/image/original/{recordId}")
    public void getOriginalImage(@PathVariable Long recordId,
        HttpServletRequest request,
        HttpServletResponse response) {
        proxyImageDownload(recordId, false, true, false, request, response);
    }

    @SaCheckPermission("sign:seal:extract")
    @GetMapping("/extract/download/{recordId}")
    public void downloadExtract(@PathVariable Long recordId,
        HttpServletRequest request,
        HttpServletResponse response) {
        proxyImageDownload(recordId, true, false, false, request, response);
    }

    /**
     * 更新提取记录文件名
     */
    @Log(module = "印章提取-更新文件名")
    @SaCheckPermission("sign:seal:extract")
    @PutMapping("/extract/{id}")
    public R<Void> updateExtractFileName(@PathVariable Long id,
        @RequestBody Map<String, String> body) {
        String newFileName = body == null ? null : body.get("extractFileName");
        if (id == null || StringUtils.isEmpty(newFileName)) {
            throw new ServiceException("参数不完整");
        }
        String displayName = newFileName.trim();
        if (!displayName.toLowerCase().endsWith(".png")) {
            displayName = displayName + ".png";
        }
        SignSealExtract record = new SignSealExtract();
        record.setId(id);
        record.setExtractFileName(displayName);
        extractMapper.updateExtractFileName(record);
        return R.ok();
    }

    /**
     * 删除提取记录（同时删除 MinIO 中的文件）
     */
    @Log(module = "印章提取-删除记录")
    @SaCheckPermission("sign:seal:remove")
    @DeleteMapping("/extract/{ids}")
    public R<Void> removeExtract(@PathVariable Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("请选择要删除的记录");
        }
        for (Long id : ids) {
            try {
                SignSealExtract record = extractMapper.selectExtractById(id);
                if (record != null) {
                    if (StringUtils.isNotEmpty(record.getExtractFilePath())) {
                        try {
                            signStorage.delete(record.getExtractFilePath());
                        } catch (Exception e) {
                            log.warn("删除提取文件失败: id={}, path={}, error={}",
                                id, record.getExtractFilePath(), e.getMessage());
                        }
                    }
                    if (StringUtils.isNotEmpty(record.getThumbnailFilePath())) {
                        try {
                            signStorage.delete(record.getThumbnailFilePath());
                        } catch (Exception e) {
                            log.warn("删除缩略图失败: id={}, path={}, error={}",
                                id, record.getThumbnailFilePath(), e.getMessage());
                        }
                    }
                    if (StringUtils.isNotEmpty(record.getRemark())) {
                        try {
                            signStorage.delete(record.getRemark());
                        } catch (Exception e) {
                            log.warn("删除原始文件失败: id={}, path={}, error={}",
                                id, record.getRemark(), e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("查询提取记录失败: id={}, error={}", id, e.getMessage());
            }
        }
        extractMapper.deleteExtractByIds(ids);
        return R.ok();
    }

    private void proxyImageDownload(Long recordId, boolean asDownload, boolean isOriginal, boolean isThumbnail,
        HttpServletRequest request,
        HttpServletResponse response) {
        try {
            String referer = request.getHeader("Referer");
            String host = request.getHeader("Host");
            if (StringUtils.isNotEmpty(referer)) {
                boolean isValidReferer = referer.contains(host)
                    || referer.contains("localhost")
                    || referer.contains("127.0.0.1");
                if (!isValidReferer) {
                    log.warn("图片请求防盗链拦截: recordId={}, referer={}", recordId, referer);
                    response.setStatus(403);
                    return;
                }
            }

            Long userId = UserContextHolder.getUserId();
            if (!sealService.canAccessExtractRecord(recordId, userId)) {
                response.setStatus(403);
                return;
            }

            SignSealExtract record = extractMapper.selectExtractById(recordId);
            if (record == null) {
                response.setStatus(404);
                return;
            }
            String objectName;
            String displayName;
            if (isOriginal) {
                objectName = record.getRemark();
                displayName = record.getOriginalFileName();
            } else if (isThumbnail) {
                objectName = record.getThumbnailFilePath();
                displayName = "thumb-" + record.getExtractFileName();
                if (StringUtils.isEmpty(objectName)) {
                    objectName = record.getExtractFilePath();
                }
            } else {
                objectName = record.getExtractFilePath();
                displayName = record.getExtractFileName();
            }
            if (StringUtils.isEmpty(objectName)) {
                response.setStatus(404);
                return;
            }
            byte[] data = signStorage.download(objectName);
            if (StringUtils.isEmpty(displayName)) {
                displayName = "seal-" + recordId + ".png";
            }
            response.setContentType("image/png");
            if (isThumbnail) {
                response.setHeader("Cache-Control", "public, max-age=3600");
            } else {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
            if (asDownload) {
                response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + displayName + "\"");
            }
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("图片代理失败", e);
            try {
                response.setStatus(500);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 印章提取记录列表
     */
    @SaCheckPermission("sign:seal:extract")
    @GetMapping("/extract/list")
    public R<PageResp<SignSealExtract>> extractList(SignSealExtract extract, @Valid PageQuery pageQuery) {
        return R.ok(PageQueryHelper.page(pageQuery, () -> {
            List<SignSealExtract> list = extractMapper.selectExtractList(extract);
            for (SignSealExtract item : list) {
                if (item.getId() != null) {
                    item.setThumbnailFileUrl("/sign/seal/extract/image/thumb/" + item.getId());
                    item.setExtractFileUrl("/sign/seal/extract/image/" + item.getId());
                    item.setOriginalFileUrl("/sign/seal/extract/image/original/" + item.getId());
                }
                String displayName = item.getExtractFileName();
                if (displayName == null || displayName.isEmpty()) {
                    if (item.getExtractFilePath() != null && item.getExtractFilePath().contains("/")) {
                        displayName = item.getExtractFilePath().substring(item.getExtractFilePath().lastIndexOf("/") + 1);
                        if (displayName.length() > 36 && displayName.charAt(32) == '.') {
                            displayName = displayName.substring(33);
                        }
                    } else {
                        displayName = "印章图片";
                    }
                }
                item.setExtractFileDisplayName(displayName);
            }
            return list;
        }));
    }

    @Log(module = "印章授权-绑定")
    @SaCheckPermission("sign:seal:auth")
    @PostMapping("/{sealId}/auth/bind")
    public R<Void> bindAuth(@PathVariable Long sealId, @RequestBody Long[] userIds) {
        sealService.bindPermissions(sealId, userIds);
        return R.ok();
    }

    @Log(module = "印章授权-移除")
    @SaCheckPermission("sign:seal:auth")
    @DeleteMapping("/{sealId}/auth/{userIds}")
    public R<Void> removeAuth(@PathVariable Long sealId, @PathVariable Long[] userIds) {
        sealService.removePermissions(sealId, userIds);
        return R.ok();
    }

    @SaCheckPermission("sign:seal:query")
    @GetMapping("/{sealId}/auth/users")
    public R<List<Long>> getAuthUsers(@PathVariable Long sealId) {
        return R.ok(sealService.selectPermittedUserIds(sealId));
    }

}
