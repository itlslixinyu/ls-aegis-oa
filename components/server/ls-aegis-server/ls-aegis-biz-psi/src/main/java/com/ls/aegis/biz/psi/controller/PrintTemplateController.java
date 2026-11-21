package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.PrintTemplateQuery;
import com.ls.aegis.biz.psi.model.req.PrintTemplateReq;
import com.ls.aegis.biz.psi.model.resp.PrintRenderResp;
import com.ls.aegis.biz.psi.model.resp.PrintTemplateDetailResp;
import com.ls.aegis.biz.psi.model.resp.PrintTemplateResp;
import com.ls.aegis.biz.psi.service.PrintTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;

import java.util.List;

/**
 * PSI 打印模板 API
 */
@Tag(name = "PSI 打印模板 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/print-template")
public class PrintTemplateController {

    private final PrintTemplateService printTemplateService;

    @Operation(summary = "分页查询打印模板")
    @SaCheckPermission("psi:printTemplate:list")
    @GetMapping
    public R<PageResp<PrintTemplateResp>> page(PrintTemplateQuery query, @Valid PageQuery pageQuery) {
        return R.ok(printTemplateService.page(query, pageQuery));
    }

    @Operation(summary = "渲染打印 HTML")
    @SaCheckPermission("psi:printTemplate:get")
    @GetMapping("/render")
    public R<PrintRenderResp> render(@RequestParam String bizType,
                                     @RequestParam Long bizId,
                                     @RequestParam(required = false) Long templateId) {
        return R.ok(printTemplateService.render(bizType, bizId, templateId));
    }

    @Operation(summary = "Word 套打下载")
    @SaCheckPermission("psi:printTemplate:get")
    @GetMapping("/render-docx")
    public ResponseEntity<byte[]> renderDocx(@RequestParam String bizType,
                                             @RequestParam Long bizId,
                                             @RequestParam(required = false) Long templateId) {
        byte[] body = printTemplateService.renderDocx(bizType, bizId, templateId);
        String filename = "psi-print-" + bizType + "-" + bizId + ".docx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .body(body);
    }

    @Operation(summary = "打印模板详情")
    @SaCheckPermission("psi:printTemplate:get")
    @GetMapping("/{id}")
    public R<PrintTemplateDetailResp> get(@PathVariable Long id) {
        return R.ok(printTemplateService.get(id));
    }

    @Operation(summary = "新增打印模板")
    @SaCheckPermission("psi:printTemplate:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PrintTemplateReq req) {
        return R.ok(printTemplateService.create(req));
    }

    @Operation(summary = "修改打印模板")
    @SaCheckPermission("psi:printTemplate:update")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody PrintTemplateReq req, @PathVariable Long id) {
        printTemplateService.update(req, id);
        return R.ok();
    }

    @Operation(summary = "删除打印模板")
    @SaCheckPermission("psi:printTemplate:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        printTemplateService.delete(List.of(id));
        return R.ok();
    }
}
