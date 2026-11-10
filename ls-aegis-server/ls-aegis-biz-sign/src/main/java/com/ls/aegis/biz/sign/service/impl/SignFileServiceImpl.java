package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.FileMagicValidator;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.model.entity.StorageDO;
import com.ls.aegis.biz.file.service.FileService;
import com.ls.aegis.biz.file.service.StorageService;
import com.ls.aegis.biz.sign.domain.SignFile;
import com.ls.aegis.biz.sign.mapper.SignFileMapper;
import com.ls.aegis.biz.sign.service.ISignFileService;
import com.ls.aegis.biz.sign.service.ISignStorageService;
import com.ls.aegis.biz.sign.utils.BytesMultipartFile;
import com.ls.aegis.common.context.UserContextHolder;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 文件管理服务实现
 */
@Service
public class SignFileServiceImpl implements ISignFileService {

    private static final Logger log = LoggerFactory.getLogger(SignFileServiceImpl.class);

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "png", "jpg", "jpeg", "gif", "bmp", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );

    private static final long MAX_FILE_SIZE = 52428800L; // 50MB

    /** 平台文件指针，避免与 sign_file 自增 ID 撞号 */
    public static final String SYS_FILE_PREFIX = "sys_file:";

    @Autowired
    private SignFileMapper fileMapper;

    @Autowired
    private ISignStorageService signStorage;

    @Autowired
    private FileService platformFileService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    @Transactional
    public SignFile uploadFile(MultipartFile file, String bizType) {
        Long userId = UserContextHolder.getUserId();
        try {
            // 先读入内存再校验/上传，避免 Undertow 临时文件被二次读取时已删除
            byte[] data = file.getBytes();
            MultipartFile cached = new BytesMultipartFile(
                "file",
                file.getOriginalFilename(),
                file.getContentType(),
                data);
            validateFile(cached);

            String sha256 = signStorage.sha256(data);

            // 2. 检查 SHA256 去重
            SignFile existing = fileMapper.selectFileBySha256(sha256);
            if (existing != null) {
                log.info("文件已存在（SHA256去重）: {}", existing.getFileName());
                return existing;
            }

            // 3. 上传到平台文件中心（走字节包装，不再碰原始 Multipart 临时文件）
            String objectName = signStorage.upload(data, file.getOriginalFilename(), bizType);

            // 4. 写入数据库记录
            SignFile signFile = new SignFile();
            signFile.setFileName(file.getOriginalFilename());
            signFile.setFilePath(objectName);
            signFile.setFileSize((long) data.length);
            signFile.setFileMime(file.getContentType());
            signFile.setFileSha256(sha256);
            signFile.setFileExt(getExtension(file.getOriginalFilename()));
            signFile.setOwnerId(userId);
            signFile.setBizType(bizType);
            signFile.setCreateTime(LocalDateTime.now());
            fileMapper.insertFile(signFile);

            log.info("文件上传成功: {} -> {}", file.getOriginalFilename(), objectName);
            return signFile;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            throw new ServiceException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传字节数组文件（内部使用，如签章结果文件）
     */
    @Transactional
    public SignFile uploadBytes(byte[] data, String fileName, String bizType) {
        Long userId = UserContextHolder.getUserId();
        String sha256 = signStorage.sha256(data);

        // 去重
        SignFile existing = fileMapper.selectFileBySha256(sha256);
        if (existing != null) {
            return existing;
        }

        String objectName = signStorage.upload(data, fileName, bizType);

        SignFile signFile = new SignFile();
        signFile.setFileName(fileName);
        signFile.setFilePath(objectName);
        signFile.setFileSize((long) data.length);
        signFile.setFileMime(getMimeType(fileName));
        signFile.setFileSha256(sha256);
        signFile.setFileExt(getExtension(fileName));
        signFile.setOwnerId(userId);
        signFile.setBizType(bizType);
        signFile.setCreateTime(LocalDateTime.now());
        fileMapper.insertFile(signFile);

        return signFile;
    }

    @Override
    public SignFile getFileInfo(Long fileId) {
        return fileMapper.selectFileById(fileId);
    }

    @Override
    public String getDownloadUrl(Long fileId) {
        SignFile file = fileMapper.selectFileById(fileId);
        if (file == null) {
            throw new ServiceException("文件不存在");
        }
        if (file.getFilePath() != null && file.getFilePath().startsWith(SYS_FILE_PREFIX)) {
            return getRelativeDownloadUrl(fileId);
        }
        return signStorage.getPresignedDownloadUrl(file.getFilePath());
    }

    @Override
    public byte[] getFileBytes(Long fileId) {
        SignFile file = fileMapper.selectFileById(fileId);
        if (file == null) {
            throw new ServiceException("文件不存在");
        }
        if (file.getFilePath() != null && file.getFilePath().startsWith(SYS_FILE_PREFIX)) {
            Long sysFileId = Long.parseLong(file.getFilePath().substring(SYS_FILE_PREFIX.length()));
            return downloadPlatformBytes(sysFileId);
        }
        return signStorage.download(file.getFilePath());
    }

    @Override
    public SignFile importFromPlatform(Long sysFileId) {
        if (sysFileId == null) {
            throw new ServiceException("平台文件ID不能为空");
        }
        String pointer = SYS_FILE_PREFIX + sysFileId;
        SignFile existing = fileMapper.selectFileByPath(pointer);
        if (existing != null) {
            return existing;
        }
        FileDO platform = platformFileService.getById(sysFileId);
        if (platform == null) {
            throw new ServiceException("平台文件不存在");
        }
        // sign_file.file_sha256 为 NOT NULL + UNIQUE：导入平台文件时必须写入内容哈希，
        // 否则 INSERT 失败会被 graceful-response 包成「服务器异常，请联系管理员」。
        byte[] bytes;
        try {
            bytes = downloadPlatformBytes(sysFileId);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("读取平台文件失败: " + e.getMessage());
        }
        if (bytes == null || bytes.length == 0) {
            throw new ServiceException("平台文件内容为空");
        }
        String sha256 = signStorage.sha256(bytes);
        SignFile byHash = fileMapper.selectFileBySha256(sha256);
        if (byHash != null) {
            return byHash;
        }
        SignFile signFile = new SignFile();
        signFile.setFileName(platform.getOriginalName() != null ? platform.getOriginalName() : ("file-" + sysFileId + ".pdf"));
        signFile.setFilePath(pointer);
        signFile.setFileSize(platform.getSize() != null ? platform.getSize() : (long) bytes.length);
        signFile.setFileMime(platform.getContentType());
        signFile.setFileExt(platform.getExtension());
        signFile.setFileSha256(sha256);
        signFile.setOwnerId(UserContextHolder.getUserId());
        signFile.setBizType("sign");
        signFile.setCreateTime(LocalDateTime.now());
        try {
            fileMapper.insertFile(signFile);
        } catch (Exception e) {
            SignFile raced = fileMapper.selectFileByPath(pointer);
            if (raced == null) {
                raced = fileMapper.selectFileBySha256(sha256);
            }
            if (raced != null) {
                return raced;
            }
            throw new ServiceException("导入平台文件失败: " + e.getMessage());
        }
        return signFile;
    }

    @Override
    public Long resolveToSignFileId(Long signFileId, Long sysFileId) {
        if (sysFileId != null) {
            return importFromPlatform(sysFileId).getFileId();
        }
        if (signFileId != null) {
            return signFileId;
        }
        throw new ServiceException("文件ID不能为空");
    }

    private byte[] downloadPlatformBytes(Long sysFileId) {
        FileDO platform = platformFileService.getById(sysFileId);
        if (platform == null) {
            throw new ServiceException("平台文件不存在");
        }
        StorageDO storage = storageService.getById(platform.getStorageId());
        if (storage == null) {
            throw new ServiceException("文件存储配置不存在");
        }
        FileInfo fileInfo = platform.toFileInfo(storage);
        return fileStorageService.download(fileInfo).bytes();
    }

    @Override
    public boolean canAccessFile(Long fileId, Long userId) {
        SignFile file = fileMapper.selectFileById(fileId);
        if (file == null) {
            return false;
        }
        if (isAdminUser(userId)) {
            return true;
        }
        return userId != null && userId.equals(file.getOwnerId());
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
    public List<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }

    /**
     * 获取文件下载URL（相对路径，用于nginx/后端直接提供）
     */
    public String getRelativeDownloadUrl(Long fileId) {
        return "/sign/file/download/" + fileId;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }

        // 1. 文件大小校验（防 PDF 炸弹 / 超大文件占满磁盘与内存）
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ServiceException("文件大小不能超过 50MB");
        }
        // 2. 空文件（大小=0）
        if (file.getSize() <= 0) {
            throw new ServiceException("文件内容为空，拒绝上传");
        }

        // 3. 扩展名校验（后缀白名单）
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (ext.isEmpty() || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ServiceException("不支持的文件类型: " + (ext.isEmpty() ? "(无扩展名)" : ext));
        }

        // 4. 【关键】魔数校验：扩展名与文件头真实内容必须一致，防止伪装文件（webshell 改 .pdf 上传等）
        FileMagicValidator.assertValid(file, ext);
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String getMimeType(String fileName) {
        String ext = getExtension(fileName);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }
}
