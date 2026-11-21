package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.StockQueryQuery;
import com.ls.aegis.biz.psi.model.resp.StockQueryResp;
import com.ls.aegis.biz.psi.model.resp.StockRestoreBatchResp;
import java.util.List;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

/**
 * 库存查询（按仓+商品汇总剩余，只读）
 */
public interface StockQueryService {

    List<StockRestoreBatchResp> restoreBatches(Long warehouseId, Long goodsId);

    PageResp<StockQueryResp> page(StockQueryQuery query, PageQuery pageQuery);
}
