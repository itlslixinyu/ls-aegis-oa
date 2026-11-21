package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.StockTransferQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockTransferReq;
import com.ls.aegis.biz.psi.model.resp.StockTransferResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

public interface StockTransferService {
    PageResp<StockTransferResp> page(StockTransferQuery query, PageQuery pageQuery);
    StockTransferResp get(Long id);
    Long create(StockTransferReq req);
    void update(StockTransferReq req, Long id);
    void delete(Long id);
    String confirm(Long id, DocConfirmReq req);
}
