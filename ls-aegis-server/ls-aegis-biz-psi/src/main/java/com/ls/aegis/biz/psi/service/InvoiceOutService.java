package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.InvoiceQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InvoiceReq;
import com.ls.aegis.biz.psi.model.resp.InvoiceImportParseResp;
import com.ls.aegis.biz.psi.model.resp.InvoiceResp;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

public interface InvoiceOutService {
    PageResp<InvoiceResp> page(InvoiceQuery query, PageQuery pageQuery);
    InvoiceResp get(Long id);
    Long create(InvoiceReq req);
    void update(InvoiceReq req, Long id);
    void delete(Long id);
    String confirm(Long id, DocConfirmReq req);
    String red(Long id, DocConfirmReq req);

    InvoiceImportParseResp parseImportExcel(MultipartFile file);
}
