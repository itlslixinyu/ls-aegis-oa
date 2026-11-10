package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.StockQueryQuery;
import com.ls.aegis.biz.psi.model.resp.StockQueryResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

/**
 * 库存查询（按仓+商品汇总剩余，只读）
 */
public interface StockQueryService {

    PageResp<StockQueryResp> page(StockQueryQuery query, PageQuery pageQuery);
}
