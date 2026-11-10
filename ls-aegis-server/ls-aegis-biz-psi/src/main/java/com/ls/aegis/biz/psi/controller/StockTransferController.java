package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.StockTransferQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockTransferReq;
import com.ls.aegis.biz.psi.model.resp.StockTransferResp;
import com.ls.aegis.biz.psi.service.StockTransferService;
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

@Tag(name = "PSI 调拨 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/stock-transfer")
public class StockTransferController {

    private final StockTransferService stockTransferService;

    @SaCheckPermission("psi:stockTransfer:list")
    @GetMapping
    public R<PageResp<StockTransferResp>> page(StockTransferQuery query, @Valid PageQuery pageQuery) {
        return R.ok(stockTransferService.page(query, pageQuery));
    }

    @Operation(summary = "导出调拨单")
    @SaCheckPermission("psi:stockTransfer:list")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(StockTransferQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, stockTransferService::page,
            com.ls.aegis.biz.psi.model.resp.StockTransferExportResp.class, "调拨单", response);
    }

    @SaCheckPermission("psi:stockTransfer:get")
    @GetMapping("/{id}")
    public R<StockTransferResp> get(@PathVariable Long id) {
        return R.ok(stockTransferService.get(id));
    }

    @SaCheckPermission("psi:stockTransfer:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody StockTransferReq req) {
        return R.ok(stockTransferService.create(req));
    }

    @SaCheckPermission("psi:stockTransfer:update")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody StockTransferReq req, @PathVariable Long id) {
        stockTransferService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:stockTransfer:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stockTransferService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-调拨确认")
    @Operation(summary = "确认调拨")
    @SaCheckPermission("psi:stockTransfer:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(stockTransferService.confirm(id, req));
    }
}
