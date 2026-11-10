package com.ls.aegis.biz.quote.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.quote.domain.Quote;
import com.ls.aegis.biz.quote.model.resp.QuoteExportResp;
import com.ls.aegis.biz.quote.model.resp.QuoteRenderResp;
import com.ls.aegis.biz.quote.model.resp.QuoteSummaryResp;
import com.ls.aegis.biz.quote.service.IQuoteExportService;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.biz.quote.service.IQuoteService;
import com.ls.aegis.common.context.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;

/**
 * 平台报价 API（主路径 /quote）
 */
@Tag(name = "报价 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/quote")
public class QuoteController {

    private final IQuoteService quoteService;
    private final IQuoteExportService quoteExportService;

    @Operation(summary = "分页查询报价")
    @SaCheckPermission("quote:list")
    @GetMapping
    public R<PageResp<Quote>> page(Quote quote, @Valid PageQuery pageQuery) {
        return R.ok(PageQueryHelper.page(pageQuery, () -> quoteService.selectQuoteList(quote)));
    }

    @Operation(summary = "工作总览摘要")
    @SaCheckPermission("quote:list")
    @GetMapping("/summary")
    public R<QuoteSummaryResp> summary() {
        return R.ok(quoteService.selectQuoteSummary());
    }

    @Operation(summary = "导出报价")
    @Log(module = "报价管理-导出")
    @SaCheckPermission("quote:export")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(HttpServletResponse response, Quote quote) {
        PageQueryHelper.startExportLimit();
        List<Quote> list = quoteService.selectQuoteList(quote);
        ExcelExportHelper.export(list, QuoteExportResp.class, "报价管理数据", response);
    }

    @Operation(summary = "报价详情")
    @SaCheckPermission("quote:query")
    @GetMapping("/{id}")
    public R<Quote> get(@PathVariable Long id) {
        return R.ok(quoteService.selectQuoteById(id));
    }

    @Operation(summary = "新增报价")
    @Log(module = "报价管理-新增")
    @SaCheckPermission("quote:add")
    @PostMapping
    public R<Long> add(@Validated @RequestBody Quote quote) {
        quote.setCreateBy(resolveOperator());
        quoteService.insertQuote(quote);
        return R.ok(quote.getId());
    }

    @Operation(summary = "修改报价")
    @Log(module = "报价管理-修改")
    @SaCheckPermission("quote:update")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody Quote quote) {
        quote.setUpdateBy(resolveOperator());
        quoteService.updateQuote(quote);
        return R.ok();
    }

    @Operation(summary = "删除报价")
    @Log(module = "报价管理-删除")
    @SaCheckPermission("quote:delete")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        quoteService.deleteQuoteByIds(ids);
        return R.ok();
    }

    @Operation(summary = "报价转工单（历史兼容，业务上报价与工单无关，勿作为主流程）")
    @Log(module = "报价转工单")
    @SaCheckPermission("quote:update")
    @PutMapping("/{id}/to-order")
    public R<Long> toOrder(@PathVariable Long id,
        @RequestParam(required = false) String address,
        @RequestParam(required = false) String remark) {
        return R.ok(quoteService.convertToOrder(id, address, remark));
    }

    @Operation(summary = "渲染报价单 HTML")
    @SaCheckPermission("quote:export")
    @GetMapping("/{id}/render")
    public R<QuoteRenderResp> render(@PathVariable Long id) {
        return R.ok(quoteExportService.renderHtml(id));
    }

    @Operation(summary = "导出报价单 Word")
    @Log(module = "报价管理-导出Word")
    @SaCheckPermission("quote:export")
    @GetMapping(value = "/{id}/export-docx", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> exportDocx(@PathVariable Long id) {
        byte[] body = quoteExportService.exportDocx(id);
        Quote quote = quoteService.selectQuoteById(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, buildAttachmentDisposition(quote, ".docx"))
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .body(body);
    }

    @Operation(summary = "导出报价单 PDF")
    @Log(module = "报价管理-导出PDF")
    @SaCheckPermission("quote:export")
    @GetMapping(value = "/{id}/export-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        byte[] body = quoteExportService.exportPdf(id);
        Quote quote = quoteService.selectQuoteById(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, buildAttachmentDisposition(quote, ".pdf"))
            .contentType(MediaType.APPLICATION_PDF)
            .body(body);
    }

    /** 导出文件名：项目名称-报价单号 */
    private String buildExportFileName(Quote quote, String ext) {
        String project = quote != null && StringUtils.hasText(quote.getProjectName())
            ? quote.getProjectName().trim()
            : "未命名项目";
        String quoteNo = quote != null && StringUtils.hasText(quote.getQuoteNo())
            ? quote.getQuoteNo().trim()
            : "quote";
        String base = (project + "-" + quoteNo).replaceAll("[\\\\/:*?\"<>|]", "_");
        return base + ext;
    }

    private String buildAttachmentDisposition(Quote quote, String ext) {
        String filename = buildExportFileName(quote, ext);
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }

    private String resolveOperator() {
        String username = UserContextHolder.getUsername();
        return StringUtils.hasText(username) ? username : "system";
    }
}
