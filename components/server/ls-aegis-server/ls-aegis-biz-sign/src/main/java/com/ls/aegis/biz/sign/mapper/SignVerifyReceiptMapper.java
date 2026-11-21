package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignVerifyReceipt;

/**
 * 验签回单 Mapper。
 */
public interface SignVerifyReceiptMapper {

    SignVerifyReceipt selectByRecordId(String recordId);

    int insertSignVerifyReceipt(SignVerifyReceipt receipt);
}
