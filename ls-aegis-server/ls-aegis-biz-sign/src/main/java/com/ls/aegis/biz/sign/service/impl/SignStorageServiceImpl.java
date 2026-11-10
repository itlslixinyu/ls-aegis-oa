package com.ls.aegis.biz.sign.service.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.model.entity.StorageDO;
import com.ls.aegis.biz.file.service.FileService;
import com.ls.aegis.biz.file.service.StorageService;
import com.ls.aegis.biz.sign.service.ISignStorageService;
import com.ls.aegis.biz.sign.utils.BytesMultipartFile;
import com.ls.aegis.compat.common.exception.ServiceException;

/**
 * 签章对象存储：写入平台 {@code sys_file}，对象名固定为 {@code sys_file:{id}}。
 */
@Service
public class SignStorageServiceImpl implements ISignStorageService {

    private static final Logger log = LoggerFactory.getLogger(SignStorageServiceImpl.class);

    public static final String SYS_FILE_PREFIX = "sys_file:";

    @Autowired
    private FileService fileService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public String upload(byte[] data, String fileName, String bizType) {
        String name = StrUtil.blankToDefault(fileName, "file.bin");
        MultipartFile file = new BytesMultipartFile("file", name, guessContentType(name), data);
        return uploadMultipart(file, bizType);
    }

    @Override
    public String uploadMultipart(MultipartFile file, String bizType) {
        try {
            FileInfo info = fileService.upload(file, parentPath(bizType));
            if (info == null || StrUtil.isBlank(info.getId())) {
                throw new ServiceException("平台文件上传未返回文件ID");
            }
            String pointer = SYS_FILE_PREFIX + info.getId();
            log.info("签章文件已写入平台: {} -> {}", file.getOriginalFilename(), pointer);
            return pointer;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("签章文件上传失败: {}", file.getOriginalFilename(), e);
            throw new ServiceException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] download(String objectName) {
        Long sysFileId = parseSysFileId(objectName);
        if (sysFileId == null) {
            throw new ServiceException("签章存储已并入平台文件中心，旧对象无法读取，请重新上传: " + objectName);
        }
        return downloadPlatformBytes(sysFileId);
    }

    @Override
    public String getPresignedDownloadUrl(String objectName) {
        Long sysFileId = parseSysFileId(objectName);
        if (sysFileId == null) {
            throw new ServiceException("签章存储已并入平台文件中心，旧对象无法读取，请重新上传: " + objectName);
        }
        FileDO file = fileService.getById(sysFileId);
        if (file == null || file.getPath() == null) {
            throw new ServiceException("平台文件不存在");
        }
        StorageDO storage = storageService.getById(file.getStorageId());
        if (storage == null) {
            throw new ServiceException("文件存储配置不存在");
        }
        return URLUtil.normalize(storage.getUrlPrefix() + file.getPath(), false, true);
    }

    @Override
    public String getPresignedUploadUrl(String objectName, int expiryMinutes) {
        throw new ServiceException("请使用 /sign/file/upload 或平台 /system/common/file 上传");
    }

    @Override
    public void delete(String objectName) {
        Long sysFileId = parseSysFileId(objectName);
        if (sysFileId == null) {
            log.warn("忽略非平台文件指针，无法删除: {}", objectName);
            return;
        }
        try {
            fileService.delete(List.of(sysFileId));
            log.info("平台文件已删除: {}", objectName);
        } catch (Exception e) {
            log.error("平台文件删除失败: {}", objectName, e);
            throw new ServiceException("文件删除失败: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String objectName) {
        Long sysFileId = parseSysFileId(objectName);
        if (sysFileId == null) {
            return false;
        }
        return fileService.getById(sysFileId) != null;
    }

    @Override
    public String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    private byte[] downloadPlatformBytes(Long sysFileId) {
        FileDO platform = fileService.getById(sysFileId);
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

    static Long parseSysFileId(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        String trimmed = objectName.trim();
        if (!trimmed.startsWith(SYS_FILE_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(trimmed.substring(SYS_FILE_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String parentPath(String bizType) {
        String type = StrUtil.blankToDefault(bizType, "sign").replaceAll("[^a-zA-Z0-9_-]", "");
        if (type.isEmpty()) {
            type = "sign";
        }
        return "/sign/" + type;
    }

    private static String guessContentType(String fileName) {
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            ext = fileName.substring(dot + 1).toLowerCase();
        }
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "application/octet-stream";
        };
    }
}
