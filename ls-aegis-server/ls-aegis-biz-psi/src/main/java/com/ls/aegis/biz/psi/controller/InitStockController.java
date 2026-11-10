package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.InitStockQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InitStockReq;
import com.ls.aegis.biz.psi.model.resp.InitStockResp;
import com.ls.aegis.biz.psi.service.InitStockService;
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

@Tag(name = "PSI 库存期初 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/init/stock")
public class InitStockController {

    private final InitStockService initStockService;

    @SaCheckPermission("psi:init:list")
    @GetMapping
    public R<PageResp<InitStockResp>> page(InitStockQuery query, @Valid PageQuery pageQuery) {
        return R.ok(initStockService.page(query, pageQuery));
    }

    @SaCheckPermission("psi:init:list")
    @GetMapping("/{id}")
    public R<InitStockResp> get(@PathVariable Long id) {
        return R.ok(initStockService.get(id));
    }

    @SaCheckPermission("psi:init:confirm")
    @PostMapping
    public R<Long> create(@Valid @RequestBody InitStockReq req) {
        return R.ok(initStockService.create(req));
    }

    @SaCheckPermission("psi:init:confirm")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody InitStockReq req, @PathVariable Long id) {
        initStockService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:init:confirm")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        initStockService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-库存期初确认")
    @Operation(summary = "确认库存期初")
    @SaCheckPermission("psi:init:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(initStockService.confirm(id, req));
    }

    @Operation(summary = "下载库存期初导入模板")
    @SaCheckPermission("psi:init:import")
    @GetMapping(value = "/import/template", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(jakarta.servlet.http.HttpServletResponse response) {
        initStockService.downloadImportTemplate(response);
    }

    @Operation(summary = "解析库存期初导入数据")
    @SaCheckPermission("psi:init:import")
    @PostMapping("/import/parse")
    public R<com.ls.aegis.biz.psi.model.resp.PsiImportParseResp> parseImport(
        @org.springframework.web.bind.annotation.RequestPart @jakarta.validation.constraints.NotNull(message = "文件不能为空") org.springframework.web.multipart.MultipartFile file) {
        top.continew.starter.core.util.validation.ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        return R.ok(initStockService.parseImport(file));
    }

    @Operation(summary = "导入库存期初（按仓库+日期分组生成草稿）")
    @SaCheckPermission("psi:init:import")
    @PostMapping("/import")
    public R<com.ls.aegis.biz.psi.model.resp.PsiImportResp> importData(@Valid @RequestBody com.ls.aegis.biz.psi.model.req.PsiImportReq req) {
        return R.ok(initStockService.importData(req));
    }
}
