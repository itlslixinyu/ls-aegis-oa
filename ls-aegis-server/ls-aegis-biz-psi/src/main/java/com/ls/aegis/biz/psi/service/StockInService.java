package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.StockInQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockInReq;
import com.ls.aegis.biz.psi.model.resp.StockInResp;
import com.ls.aegis.biz.psi.model.resp.StockLineImportParseResp;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

/**
 * 入库单服务
 */
public interface StockInService {

    PageResp<StockInResp> page(StockInQuery query, PageQuery pageQuery);

    StockInResp get(Long id);

    Long create(StockInReq req);

    void update(StockInReq req, Long id);

    void delete(Long id);

    /**
     * 确认生效
     *
     * @return 单号
     */
    String confirm(Long id, DocConfirmReq req);

    void downloadImportTemplate(HttpServletResponse response);

    StockLineImportParseResp parseImportExcel(MultipartFile file);
}
