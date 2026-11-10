package com.ls.aegis.biz.quote.service.impl;

import com.ls.aegis.biz.quote.domain.Quote;
import com.ls.aegis.biz.quote.model.resp.QuoteRenderResp;
import com.ls.aegis.biz.quote.service.IQuoteExportService;
import com.ls.aegis.biz.quote.service.IQuoteService;
import com.ls.aegis.biz.quote.support.QuoteAmountCalculator;
import com.ls.aegis.biz.quote.support.QuoteDocxExporter;
import com.ls.aegis.biz.quote.support.QuoteDocumentRenderer;
import com.ls.aegis.biz.quote.support.QuoteLogoHelper;
import com.ls.aegis.biz.quote.support.QuotePdfExporter;
import com.ls.aegis.common.api.system.SysOptionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.continew.starter.core.exception.BusinessException;

/**
 * 报价单文档导出实现
 */
@Service
@RequiredArgsConstructor
public class QuoteExportServiceImpl implements IQuoteExportService {

    private final IQuoteService quoteService;
    private final SysOptionApi sysOptionApi;

    @Override
    public QuoteRenderResp renderHtml(Long id) {
        Quote quote = requireQuote(id);
        QuoteRenderResp resp = new QuoteRenderResp();
        resp.setTitle(buildTitle(quote));
        resp.setHtml(QuoteDocumentRenderer.renderHtml(quote, resolveLogoUrl()));
        return resp;
    }

    @Override
    public byte[] exportDocx(Long id) {
        Quote quote = requireQuote(id);
        return QuoteDocxExporter.export(quote, resolveLogoUrl());
    }

    @Override
    public byte[] exportPdf(Long id) {
        Quote quote = requireQuote(id);
        String html = QuoteDocumentRenderer.renderHtml(quote, resolveLogoUrl());
        return QuotePdfExporter.htmlToPdf(html);
    }

    private Quote requireQuote(Long id) {
        if (id == null) {
            throw new BusinessException("报价单ID不能为空");
        }
        Quote quote = quoteService.selectQuoteById(id);
        if (quote == null) {
            throw new BusinessException("报价单不存在");
        }
        QuoteAmountCalculator.applyTotals(quote, quote.getDetails());
        return quote;
    }

    private String buildTitle(Quote quote) {
        return StringUtils.hasText(quote.getQuoteNo()) ? "报价单-" + quote.getQuoteNo() : "报价单";
    }

    private String resolveLogoUrl() {
        String logo = sysOptionApi.getValue("SITE", "SITE_LOGO");
        return QuoteLogoHelper.embeddableLogoSrc(logo);
    }
}
