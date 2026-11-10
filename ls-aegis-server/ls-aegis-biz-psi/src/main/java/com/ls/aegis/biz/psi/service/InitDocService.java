package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.InitDocQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InitDocReq;
import com.ls.aegis.biz.psi.model.resp.InitDocResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

public interface InitDocService {
    PageResp<InitDocResp> page(InitDocQuery query, PageQuery pageQuery);
    InitDocResp get(String type, Long id);
    Long create(InitDocReq req);
    void update(InitDocReq req, Long id);
    void delete(String type, Long id);
    String confirm(String type, Long id, DocConfirmReq req);
}
