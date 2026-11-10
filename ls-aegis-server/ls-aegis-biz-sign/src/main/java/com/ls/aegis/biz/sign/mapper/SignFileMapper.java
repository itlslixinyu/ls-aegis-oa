package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignFile;

import java.util.List;

/**
 * 文件管理Mapper接口
 */
public interface SignFileMapper {

    /**
     * 查询文件列表
     */
    List<SignFile> selectFileList(SignFile file);

    /**
     * 查询文件详情
     */
    SignFile selectFileById(Long fileId);

    /**
     * 根据SHA256查询文件
     */
    SignFile selectFileBySha256(String sha256);

    /**
     * 按存储路径查询（含平台文件指针 sys_file:{id}）
     */
    SignFile selectFileByPath(String filePath);

    /**
     * 新增文件记录
     */
    int insertFile(SignFile file);

    /**
     * 删除文件记录
     */
    int deleteFileByIds(Long[] fileIds);
}
