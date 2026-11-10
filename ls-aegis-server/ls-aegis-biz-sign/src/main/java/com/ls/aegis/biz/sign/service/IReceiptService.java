package com.ls.aegis.biz.sign.service;

import com.ls.aegis.biz.sign.domain.SignVerifyReceipt;

/**
 * 验签回单服务。
 */
public interface IReceiptService {

    SignVerifyReceipt buildReceipt(String recordId, Long originFileId, Long signedFileId,
                                   Long certId, String serialNo, String fingerprintSha256);

    SignVerifyReceipt selectByRecordId(String recordId);

    byte[] renderReceiptPdf(SignVerifyReceipt receipt);
}
