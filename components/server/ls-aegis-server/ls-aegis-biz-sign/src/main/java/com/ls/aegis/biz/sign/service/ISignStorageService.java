package com.ls.aegis.biz.sign.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 签章对象存储：写入平台 {@code sys_file}，对象名为 {@code sys_file:{id}}。
 */
public interface ISignStorageService {

    /**
     * 上传文件，返回 {@code sys_file:{id}}
     */
    String upload(byte[] data, String fileName, String bizType);

    /**
     * 上传 MultipartFile
     */
    String uploadMultipart(MultipartFile file, String bizType);

    /**
     * 下载文件
     */
    byte[] download(String objectName);

    /**
     * 生成预签名下载链接
     */
    String getPresignedDownloadUrl(String objectName);

    /**
     * 生成预签名上传链接
     */
    String getPresignedUploadUrl(String objectName, int expiryMinutes);

    /**
     * 删除文件
     */
    void delete(String objectName);

    /**
     * 检查文件是否存在
     */
    boolean exists(String objectName);

    /**
     * 计算 SHA256
     */
    String sha256(byte[] data);
}
