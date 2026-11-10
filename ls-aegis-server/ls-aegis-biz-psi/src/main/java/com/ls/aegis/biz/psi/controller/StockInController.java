package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.StockInQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockInReq;
import com.ls.aegis.biz.psi.model.resp.StockInExportResp;
import com.ls.aegis.biz.psi.model.resp.StockInResp;
import com.ls.aegis.biz.psi.service.StockInService;
import com.ls.aegis.biz.psi.support.PsiExportHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

@Tag(name = "PSI 入库单 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/stock-in")
public class StockInController {

    private final StockInService stockInService;

    @Operation(summary = "分页查询入库单")
    @SaCheckPermission("psi:stockIn:list")
    @GetMapping
    public R<PageResp<StockInResp>> page(StockInQuery query, @Valid PageQuery pageQuery) {
        return R.ok(stockInService.page(query, pageQuery));
    }

    @Operation(summary = "导出入库单")
    @SaCheckPermission("psi:stockIn:list")
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(StockInQuery query, HttpServletResponse response) {
        PsiExportHelper.export(query, stockInService::page, StockInExportResp.class, "入库单", response);
    }

    @Operation(summary = "下载出入库明细导入模板")
    @SaCheckPermission("psi:stockIn:create")
    @GetMapping(value = "/import/template", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) {
        stockInService.downloadImportTemplate(response);
    }

    @Operation(summary = "解析出入库明细 Excel")
    @SaCheckPermission("psi:stockIn:create")
    @PostMapping("/import/parse")
    public R<com.ls.aegis.biz.psi.model.resp.StockLineImportParseResp> parseImport(
        @org.springframework.web.bind.annotation.RequestPart @jakarta.validation.constraints.NotNull(message = "文件不能为空") org.springframework.web.multipart.MultipartFile file) {
        top.continew.starter.core.util.validation.ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        return R.ok(stockInService.parseImportExcel(file));
    }

    @Operation(summary = "入库单详情")
    @SaCheckPermission("psi:stockIn:get")
    @GetMapping("/{id}")
    public R<StockInResp> get(@PathVariable Long id) {
        return R.ok(stockInService.get(id));
    }

    @Operation(summary = "新增入库草稿")
    @SaCheckPermission("psi:stockIn:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody StockInReq req) {
        return R.ok(stockInService.create(req));
    }

    @Operation(summary = "修改入库草稿")
    @SaCheckPermission("psi:stockIn:update")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody StockInReq req, @PathVariable Long id) {
        stockInService.update(req, id);
        return R.ok();
    }

    @Operation(summary = "删除入库草稿")
    @SaCheckPermission("psi:stockIn:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stockInService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-入库确认")
    @Operation(summary = "确认入库生效")
    @SaCheckPermission("psi:stockIn:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(stockInService.confirm(id, req));
    }
}
