package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.StockFlowQuery;
import com.ls.aegis.biz.psi.model.resp.StockFlowResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

/**
 * 库存流水（只读）
 */
public interface StockFlowService {

    PageResp<StockFlowResp> page(StockFlowQuery query, PageQuery pageQuery);
}
