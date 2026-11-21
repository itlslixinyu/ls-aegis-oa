package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.StockReturnQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockReturnReq;
import com.ls.aegis.biz.psi.model.resp.StockReturnResp;
import com.ls.aegis.biz.psi.service.StockReturnService;
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

@Tag(name = "PSI 退货 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/stock-return")
public class StockReturnController {

    private final StockReturnService stockReturnService;

    @SaCheckPermission("psi:stockReturn:list")
    @GetMapping
    public R<PageResp<StockReturnResp>> page(StockReturnQuery query, @Valid PageQuery pageQuery) {
        return R.ok(stockReturnService.page(query, pageQuery));
    }

    @Operation(summary = "导出退货单")
    @SaCheckPermission("psi:stockReturn:list")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(StockReturnQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, stockReturnService::page,
            com.ls.aegis.biz.psi.model.resp.StockReturnExportResp.class, "退货单", response);
    }

    @SaCheckPermission("psi:stockReturn:get")
    @GetMapping("/{id}")
    public R<StockReturnResp> get(@PathVariable Long id) {
        return R.ok(stockReturnService.get(id));
    }

    @SaCheckPermission("psi:stockReturn:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody StockReturnReq req) {
        return R.ok(stockReturnService.create(req));
    }

    @SaCheckPermission("psi:stockReturn:update")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody StockReturnReq req, @PathVariable Long id) {
        stockReturnService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:stockReturn:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stockReturnService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-退货确认")
    @Operation(summary = "确认退货")
    @SaCheckPermission("psi:stockReturn:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(stockReturnService.confirm(id, req));
    }
}
