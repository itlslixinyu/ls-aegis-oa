package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.StockCheckQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockCheckReq;
import com.ls.aegis.biz.psi.model.resp.StockCheckBookInfoResp;
import com.ls.aegis.biz.psi.model.resp.StockCheckImportParseResp;
import com.ls.aegis.biz.psi.model.resp.StockCheckResp;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

public interface StockCheckService {
    PageResp<StockCheckResp> page(StockCheckQuery query, PageQuery pageQuery);
    StockCheckResp get(Long id);
    Long create(StockCheckReq req);
    void update(StockCheckReq req, Long id);
    void delete(Long id);
    String confirm(Long id, DocConfirmReq req);

    void downloadImportTemplate(HttpServletResponse response);

    /**
     * 解析实盘 Excel，可选按仓库回填账面数量
     */
    StockCheckImportParseResp parseImportExcel(MultipartFile file, Long warehouseId);

    /**
     * 按仓库+商品带出账面数量与参考未税单价
     */
    StockCheckBookInfoResp bookInfo(Long warehouseId, Long goodsId);
}
