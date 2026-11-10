package com.ls.aegis.biz.sign.service;

import com.ls.aegis.biz.sign.domain.SignFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件管理服务接口
 */
public interface ISignFileService {

    /**
     * 上传文件（SHA256去重）
     */
    SignFile uploadFile(MultipartFile file, String bizType);

    /**
     * 获取文件信息
     */
    SignFile getFileInfo(Long fileId);

    /**
     * 获取文件下载URL
     */
    String getDownloadUrl(Long fileId);

    /**
     * 获取文件字节数组
     */
    byte[] getFileBytes(Long fileId);

    /**
     * 将平台 sys_file 登记为签章源文件（file_path = sys_file:{id}）
     */
    SignFile importFromPlatform(Long sysFileId);

    /**
     * sysFileId 优先；否则沿用存量 sign_file.id
     */
    Long resolveToSignFileId(Long signFileId, Long sysFileId);

    /**
     * 判断当前用户是否可访问文件
     */
    boolean canAccessFile(Long fileId, Long userId);

    /**
     * 获取允许上传的文件类型
     */
    List<String> getAllowedExtensions();
}
