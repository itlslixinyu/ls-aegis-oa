package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.StockAdjustQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockAdjustReq;
import com.ls.aegis.biz.psi.model.resp.StockAdjustResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

public interface StockAdjustService {
    PageResp<StockAdjustResp> page(StockAdjustQuery query, PageQuery pageQuery);
    StockAdjustResp get(Long id);
    Long create(StockAdjustReq req);
    void update(StockAdjustReq req, Long id);
    void delete(Long id);
    String confirm(Long id, DocConfirmReq req);
}
