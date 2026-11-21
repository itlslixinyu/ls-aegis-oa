package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.OpenItemQuery;
import com.ls.aegis.biz.psi.model.resp.OpenItemOptionResp;
import com.ls.aegis.biz.psi.model.resp.OpenItemResp;
import com.ls.aegis.biz.psi.service.OpenItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;

import java.util.List;

@Tag(name = "PSI 应收应付台账 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi")
public class OpenItemController {

    private final OpenItemService openItemService;

    @Operation(summary = "应收台账分页")
    @SaCheckPermission("psi:receivable:list")
    @GetMapping("/receivable")
    public R<PageResp<OpenItemResp>> pageReceivable(OpenItemQuery query, @Valid PageQuery pageQuery) {
        return R.ok(openItemService.pageReceivable(query, pageQuery));
    }

    @Operation(summary = "导出应收台账")
    @SaCheckPermission("psi:receivable:list")
    @GetMapping(value = "/receivable/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportReceivable(OpenItemQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, openItemService::pageReceivable,
            com.ls.aegis.biz.psi.model.resp.OpenItemExportResp.class, "应收台账", response);
    }

    @Operation(summary = "应付台账分页")
    @SaCheckPermission("psi:payable:list")
    @GetMapping("/payable")
    public R<PageResp<OpenItemResp>> pagePayable(OpenItemQuery query, @Valid PageQuery pageQuery) {
        return R.ok(openItemService.pagePayable(query, pageQuery));
    }

    @Operation(summary = "导出应付台账")
    @SaCheckPermission("psi:payable:list")
    @GetMapping(value = "/payable/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportPayable(OpenItemQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, openItemService::pagePayable,
            com.ls.aegis.biz.psi.model.resp.OpenItemExportResp.class, "应付台账", response);
    }

    @Operation(summary = "未结清应收下拉（核销选单）")
    @SaCheckPermission("psi:receipt:writeoff")
    @GetMapping("/options/open-receivables")
    public R<List<OpenItemOptionResp>> openReceivables(@RequestParam(required = false) Long unitId) {
        return R.ok(openItemService.openReceivables(unitId));
    }

    @Operation(summary = "未结清应付下拉（核销选单）")
    @SaCheckPermission("psi:payment:writeoff")
    @GetMapping("/options/open-payables")
    public R<List<OpenItemOptionResp>> openPayables(@RequestParam(required = false) Long unitId) {
        return R.ok(openItemService.openPayables(unitId));
    }
}
