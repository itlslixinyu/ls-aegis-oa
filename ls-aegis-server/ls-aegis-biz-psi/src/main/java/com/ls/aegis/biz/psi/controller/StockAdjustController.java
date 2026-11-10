package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.StockAdjustQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockAdjustReq;
import com.ls.aegis.biz.psi.model.resp.StockAdjustResp;
import com.ls.aegis.biz.psi.service.StockAdjustService;
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

@Tag(name = "PSI 库存调整 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/stock-adjust")
public class StockAdjustController {

    private final StockAdjustService stockAdjustService;

    @SaCheckPermission("psi:adjust:list")
    @GetMapping
    public R<PageResp<StockAdjustResp>> page(StockAdjustQuery query, @Valid PageQuery pageQuery) {
        return R.ok(stockAdjustService.page(query, pageQuery));
    }

    @Operation(summary = "导出库存调整")
    @SaCheckPermission("psi:adjust:list")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(StockAdjustQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, stockAdjustService::page,
            com.ls.aegis.biz.psi.model.resp.StockAdjustExportResp.class, "库存调整", response);
    }

    @SaCheckPermission("psi:adjust:list")
    @GetMapping("/{id}")
    public R<StockAdjustResp> get(@PathVariable Long id) {
        return R.ok(stockAdjustService.get(id));
    }

    @SaCheckPermission("psi:adjust:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody StockAdjustReq req) {
        return R.ok(stockAdjustService.create(req));
    }

    @SaCheckPermission("psi:adjust:create")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody StockAdjustReq req, @PathVariable Long id) {
        stockAdjustService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:adjust:create")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stockAdjustService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-调整确认")
    @Operation(summary = "确认调整")
    @SaCheckPermission("psi:adjust:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(stockAdjustService.confirm(id, req));
    }
}
