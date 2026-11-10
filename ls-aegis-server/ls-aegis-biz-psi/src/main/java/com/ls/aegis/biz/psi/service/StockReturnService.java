package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.StockReturnQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockReturnReq;
import com.ls.aegis.biz.psi.model.resp.StockReturnResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

public interface StockReturnService {
    PageResp<StockReturnResp> page(StockReturnQuery query, PageQuery pageQuery);
    StockReturnResp get(Long id);
    Long create(StockReturnReq req);
    void update(StockReturnReq req, Long id);
    void delete(Long id);
    String confirm(Long id, DocConfirmReq req);
}
