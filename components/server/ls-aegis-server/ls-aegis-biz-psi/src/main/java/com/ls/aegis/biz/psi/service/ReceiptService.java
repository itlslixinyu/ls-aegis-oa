package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.ReceiptQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.ReceiptReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReverseReq;
import com.ls.aegis.biz.psi.model.resp.ReceiptResp;
import com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.util.List;

public interface ReceiptService {
    PageResp<ReceiptResp> page(ReceiptQuery query, PageQuery pageQuery);
    ReceiptResp get(Long id);
    Long create(ReceiptReq req);
    void update(ReceiptReq req, Long id);
    void delete(Long id);
    String confirm(Long id, DocConfirmReq req);
    Long writeOff(Long id, WriteOffReq req);
    Long reverse(Long id, WriteOffReverseReq req);
    List<WriteOffSummaryResp> listWriteOffs(Long id);
}
