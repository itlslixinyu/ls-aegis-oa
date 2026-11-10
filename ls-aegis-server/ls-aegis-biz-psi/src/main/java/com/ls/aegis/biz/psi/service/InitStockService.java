package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.InitStockQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InitStockReq;
import com.ls.aegis.biz.psi.model.resp.InitStockResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

public interface InitStockService {
    PageResp<InitStockResp> page(InitStockQuery query, PageQuery pageQuery);
    InitStockResp get(Long id);
    Long create(InitStockReq req);
    void update(InitStockReq req, Long id);
    void delete(Long id);
    String confirm(Long id, DocConfirmReq req);

    void downloadImportTemplate(jakarta.servlet.http.HttpServletResponse response);

    com.ls.aegis.biz.psi.model.resp.PsiImportParseResp parseImport(org.springframework.web.multipart.MultipartFile file);

    com.ls.aegis.biz.psi.model.resp.PsiImportResp importData(com.ls.aegis.biz.psi.model.req.PsiImportReq req);
}
