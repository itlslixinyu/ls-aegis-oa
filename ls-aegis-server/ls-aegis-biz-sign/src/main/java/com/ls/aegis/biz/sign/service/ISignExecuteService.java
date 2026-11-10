package com.ls.aegis.biz.sign.service;

import com.ls.aegis.biz.sign.domain.SignVerifyReceipt;
import com.ls.aegis.biz.sign.domain.vo.SignRequest;
import com.ls.aegis.biz.sign.domain.vo.SignResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * PDF签章服务接口
 */
public interface ISignExecuteService {

    /**
     * 执行签章
     * 流程：文件校验 → 权限校验 → PDF叠加印章 → PKCS7数字签名 → 保存文件 → 记录日志
     *
     * @param request 签章请求
     * @return 签章结果
     */
    SignResult executeSign(SignRequest request);

    /**
     * 从已签章PDF提取证书信息
     */
    String extractCertInfo(Long fileId);

    /**
     * 指纹校验 - 验证签章PDF完整性
     */
    boolean verifyFingerprint(Long fileId);

    /**
     * 上传PDF并执行指纹+CA校验，返回完整元数据
     * @param file 签章后的PDF文件
     * @return 校验结果（包含 fingerprint / ca 两组数据）
     */
    Map<String, Object> verifyFingerprintUpload(MultipartFile file);

    /**
     * 获取可签章项目列表
     */
    Object getSignableProjects(Long userId);

    /**
     * 签章准备 - 校验文件、项目、印章权限
     */
    boolean prepareSign(SignRequest request);

    /**
     * 查询签章回单快照
     */
    SignVerifyReceipt selectReceiptByRecordId(String recordId);
}
