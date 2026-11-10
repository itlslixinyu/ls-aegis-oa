package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.StockOutQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockOutReq;
import com.ls.aegis.biz.psi.model.resp.StockLineImportParseResp;
import com.ls.aegis.biz.psi.model.resp.StockOutResp;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

/**
 * 出库单服务
 */
public interface StockOutService {

    PageResp<StockOutResp> page(StockOutQuery query, PageQuery pageQuery);

    StockOutResp get(Long id);

    Long create(StockOutReq req);

    void update(StockOutReq req, Long id);

    void delete(Long id);

    /**
     * 确认生效（FIFO）
     *
     * @return 单号
     */
    String confirm(Long id, DocConfirmReq req);

    void downloadImportTemplate(HttpServletResponse response);

    StockLineImportParseResp parseImportExcel(MultipartFile file);
}
