package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.PaymentQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.PaymentReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReverseReq;
import com.ls.aegis.biz.psi.model.resp.PaymentResp;
import com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.util.List;

public interface PaymentService {
    PageResp<PaymentResp> page(PaymentQuery query, PageQuery pageQuery);
    PaymentResp get(Long id);
    Long create(PaymentReq req);
    void update(PaymentReq req, Long id);
    void delete(Long id);
    String confirm(Long id, DocConfirmReq req);
    Long writeOff(Long id, WriteOffReq req);
    Long reverse(Long id, WriteOffReverseReq req);
    List<WriteOffSummaryResp> listWriteOffs(Long id);
}
