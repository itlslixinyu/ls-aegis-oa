package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.InvoiceQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InvoiceReq;
import com.ls.aegis.biz.psi.model.resp.InvoiceResp;
import com.ls.aegis.biz.psi.service.InvoiceOutService;
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

@Tag(name = "PSI 销项发票 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/invoice-out")
public class InvoiceOutController {

    private final InvoiceOutService invoiceOutService;

    @SaCheckPermission("psi:invoice-out:list")
    @GetMapping
    public R<PageResp<InvoiceResp>> page(InvoiceQuery query, @Valid PageQuery pageQuery) {
        return R.ok(invoiceOutService.page(query, pageQuery));
    }

    @Operation(summary = "导出销项发票")
    @SaCheckPermission("psi:invoice-out:list")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(InvoiceQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, invoiceOutService::page,
            com.ls.aegis.biz.psi.model.resp.InvoiceExportResp.class, "销项发票", response);
    }

    @Operation(summary = "解析税务发票文件（XML/PDF/OFD）")
    @SaCheckPermission("psi:invoice-out:create")
    @PostMapping("/import/parse")
    public R<com.ls.aegis.biz.psi.model.resp.InvoiceImportParseResp> parseImport(
        @org.springframework.web.bind.annotation.RequestPart @jakarta.validation.constraints.NotNull(message = "文件不能为空") org.springframework.web.multipart.MultipartFile file) {
        top.continew.starter.core.util.validation.ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        return R.ok(invoiceOutService.parseImportExcel(file));
    }

    @SaCheckPermission("psi:invoice-out:list")
    @GetMapping("/{id}")
    public R<InvoiceResp> get(@PathVariable Long id) {
        return R.ok(invoiceOutService.get(id));
    }

    @SaCheckPermission("psi:invoice-out:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody InvoiceReq req) {
        return R.ok(invoiceOutService.create(req));
    }

    @SaCheckPermission("psi:invoice-out:create")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody InvoiceReq req, @PathVariable Long id) {
        invoiceOutService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:invoice-out:create")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        invoiceOutService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-销项确认")
    @SaCheckPermission("psi:invoice-out:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(invoiceOutService.confirm(id, req));
    }

    @Log(module = "PSI-销项冲红")
    @Operation(summary = "冲红")
    @SaCheckPermission("psi:invoice-out:red")
    @PostMapping("/{id}/red")
    public R<String> red(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(invoiceOutService.red(id, req));
    }
}
