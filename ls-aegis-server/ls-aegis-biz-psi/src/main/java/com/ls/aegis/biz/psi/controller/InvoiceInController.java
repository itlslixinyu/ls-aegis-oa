package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.InvoiceQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InvoiceReq;
import com.ls.aegis.biz.psi.model.resp.InvoiceResp;
import com.ls.aegis.biz.psi.service.InvoiceInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;

@Tag(name = "PSI 进项发票 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/invoice-in")
public class InvoiceInController {

    private final InvoiceInService invoiceInService;

    @SaCheckPermission("psi:invoice-in:list")
    @GetMapping
    public R<PageResp<InvoiceResp>> page(InvoiceQuery query, @Valid PageQuery pageQuery) {
        return R.ok(invoiceInService.page(query, pageQuery));
    }

    @Operation(summary = "导出进项发票")
    @SaCheckPermission("psi:invoice-in:list")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(InvoiceQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, invoiceInService::page,
            com.ls.aegis.biz.psi.model.resp.InvoiceExportResp.class, "进项发票", response);
    }

    @Operation(summary = "解析税务发票文件（XML/PDF/OFD）")
    @SaCheckPermission("psi:invoice-in:create")
    @PostMapping("/import/parse")
    public R<com.ls.aegis.biz.psi.model.resp.InvoiceImportParseResp> parseImport(
        @org.springframework.web.bind.annotation.RequestPart @jakarta.validation.constraints.NotNull(message = "文件不能为空") org.springframework.web.multipart.MultipartFile file) {
        top.continew.starter.core.util.validation.ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        return R.ok(invoiceInService.parseImportExcel(file));
    }

    @SaCheckPermission("psi:invoice-in:list")
    @GetMapping("/{id}")
    public R<InvoiceResp> get(@PathVariable Long id) {
        return R.ok(invoiceInService.get(id));
    }

    @SaCheckPermission("psi:invoice-in:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody InvoiceReq req) {
        return R.ok(invoiceInService.create(req));
    }

    @SaCheckPermission("psi:invoice-in:create")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody InvoiceReq req, @PathVariable Long id) {
        invoiceInService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:invoice-in:create")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        invoiceInService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-进项确认")
    @SaCheckPermission("psi:invoice-in:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(invoiceInService.confirm(id, req));
    }

    @Log(module = "PSI-进项冲红")
    @Operation(summary = "冲红")
    @SaCheckPermission("psi:invoice-in:red")
    @PostMapping("/{id}/red")
    public R<String> red(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(invoiceInService.red(id, req));
    }
}
