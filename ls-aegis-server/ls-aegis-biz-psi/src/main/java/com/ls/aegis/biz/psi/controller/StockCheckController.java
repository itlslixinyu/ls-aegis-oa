package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ls.aegis.biz.psi.model.query.StockCheckQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockCheckReq;
import com.ls.aegis.biz.psi.model.resp.StockCheckBookInfoResp;
import com.ls.aegis.biz.psi.model.resp.StockCheckImportParseResp;
import com.ls.aegis.biz.psi.model.resp.StockCheckResp;
import com.ls.aegis.biz.psi.service.StockCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.util.validation.ValidationUtils;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;

@Tag(name = "PSI 盘点 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/stock-check")
public class StockCheckController {

    private final StockCheckService stockCheckService;

    @SaCheckPermission("psi:check:list")
    @GetMapping
    public R<PageResp<StockCheckResp>> page(StockCheckQuery query, @Valid PageQuery pageQuery) {
        return R.ok(stockCheckService.page(query, pageQuery));
    }

    @Operation(summary = "导出盘点单")
    @SaCheckPermission("psi:check:list")
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(StockCheckQuery query, HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, stockCheckService::page,
            com.ls.aegis.biz.psi.model.resp.StockCheckExportResp.class, "盘点单", response);
    }

    @Operation(summary = "下载盘点实盘导入模板")
    @SaCheckPermission("psi:check:create")
    @GetMapping(value = "/import/template", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) {
        stockCheckService.downloadImportTemplate(response);
    }

    @Operation(summary = "解析盘点实盘 Excel")
    @SaCheckPermission("psi:check:create")
    @PostMapping("/import/parse")
    public R<StockCheckImportParseResp> parseImport(
        @RequestPart @NotNull(message = "文件不能为空") MultipartFile file,
        @RequestParam(required = false) Long warehouseId) {
        ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        return R.ok(stockCheckService.parseImportExcel(file, warehouseId));
    }

    @Operation(summary = "按仓库+商品带出账面数量与参考未税单价")
    @SaCheckPermission(value = {"psi:check:list", "psi:check:create"}, mode = SaMode.OR)
    @GetMapping("/book-info")
    public R<StockCheckBookInfoResp> bookInfo(
        @RequestParam @NotNull(message = "仓库不能为空") Long warehouseId,
        @RequestParam @NotNull(message = "商品不能为空") Long goodsId) {
        return R.ok(stockCheckService.bookInfo(warehouseId, goodsId));
    }

    @SaCheckPermission("psi:check:list")
    @GetMapping("/{id}")
    public R<StockCheckResp> get(@PathVariable Long id) {
        return R.ok(stockCheckService.get(id));
    }

    @SaCheckPermission("psi:check:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody StockCheckReq req) {
        return R.ok(stockCheckService.create(req));
    }

    @SaCheckPermission("psi:check:create")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody StockCheckReq req, @PathVariable Long id) {
        stockCheckService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:check:create")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stockCheckService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-盘点确认")
    @Operation(summary = "确认盘点")
    @SaCheckPermission("psi:check:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(stockCheckService.confirm(id, req));
    }
}
