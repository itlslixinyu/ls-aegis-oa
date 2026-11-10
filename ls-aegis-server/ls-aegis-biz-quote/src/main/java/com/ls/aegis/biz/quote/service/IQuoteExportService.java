package com.ls.aegis.biz.quote.service;

import com.ls.aegis.biz.quote.model.resp.QuoteRenderResp;

/**
 * 报价单文档导出（HTML / Word / PDF）
 */
public interface IQuoteExportService {

    QuoteRenderResp renderHtml(Long id);

    byte[] exportDocx(Long id);

    byte[] exportPdf(Long id);
}
