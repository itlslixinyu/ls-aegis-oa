package com.ls.aegis.biz.psi.controller;

import com.ls.aegis.biz.psi.model.query.StockOutQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockOutReq;
import com.ls.aegis.biz.psi.model.resp.StockOutResp;
import com.ls.aegis.biz.psi.service.StockOutService;
import cn.dev33.satoken.annotation.SaCheckPermission;
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

/**
 * 出库单 API
 */
@Tag(name = "PSI 出库单 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/stock-out")
public class StockOutController {

    private final StockOutService stockOutService;

    @Operation(summary = "分页查询出库单")
    @SaCheckPermission("psi:stockOut:list")
    @GetMapping
    public R<PageResp<StockOutResp>> page(StockOutQuery query, @Valid PageQuery pageQuery) {
        return R.ok(stockOutService.page(query, pageQuery));
    }

    @Operation(summary = "导出出库单")
    @SaCheckPermission("psi:stockOut:list")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(StockOutQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, stockOutService::page,
            com.ls.aegis.biz.psi.model.resp.StockOutExportResp.class, "出库单", response);
    }

    @Operation(summary = "下载出入库明细导入模板")
    @SaCheckPermission("psi:stockOut:create")
    @GetMapping(value = "/import/template", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(jakarta.servlet.http.HttpServletResponse response) {
        stockOutService.downloadImportTemplate(response);
    }

    @Operation(summary = "解析出入库明细 Excel")
    @SaCheckPermission("psi:stockOut:create")
    @PostMapping("/import/parse")
    public R<com.ls.aegis.biz.psi.model.resp.StockLineImportParseResp> parseImport(
        @org.springframework.web.bind.annotation.RequestPart @jakarta.validation.constraints.NotNull(message = "文件不能为空") org.springframework.web.multipart.MultipartFile file) {
        top.continew.starter.core.util.validation.ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        return R.ok(stockOutService.parseImportExcel(file));
    }

    @Operation(summary = "出库单详情")
    @SaCheckPermission("psi:stockOut:get")
    @GetMapping("/{id}")
    public R<StockOutResp> get(@PathVariable Long id) {
        return R.ok(stockOutService.get(id));
    }

    @Operation(summary = "新增出库草稿")
    @SaCheckPermission("psi:stockOut:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody StockOutReq req) {
        return R.ok(stockOutService.create(req));
    }

    @Operation(summary = "修改出库草稿")
    @SaCheckPermission("psi:stockOut:update")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody StockOutReq req, @PathVariable Long id) {
        stockOutService.update(req, id);
        return R.ok();
    }

    @Operation(summary = "删除出库草稿")
    @SaCheckPermission("psi:stockOut:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stockOutService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-出库确认")
    @Operation(summary = "确认出库生效（FIFO）")
    @SaCheckPermission("psi:stockOut:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(stockOutService.confirm(id, req));
    }
}
