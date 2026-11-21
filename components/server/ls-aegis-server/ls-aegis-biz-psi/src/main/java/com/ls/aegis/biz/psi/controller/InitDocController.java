package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.InitDocQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InitDocReq;
import com.ls.aegis.biz.psi.model.resp.InitDocResp;
import com.ls.aegis.biz.psi.service.InitDocService;
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

@Tag(name = "PSI 期初建账 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/init")
public class InitDocController {

    private final InitDocService initDocService;

    @SaCheckPermission("psi:init:list")
    @GetMapping
    public R<PageResp<InitDocResp>> page(InitDocQuery query, @Valid PageQuery pageQuery) {
        return R.ok(initDocService.page(query, pageQuery));
    }

    @SaCheckPermission("psi:init:list")
    @GetMapping("/{type}/{id}")
    public R<InitDocResp> get(@PathVariable String type, @PathVariable Long id) {
        return R.ok(initDocService.get(type, id));
    }

    @SaCheckPermission("psi:init:confirm")
    @PostMapping
    public R<Long> create(@Valid @RequestBody InitDocReq req) {
        return R.ok(initDocService.create(req));
    }

    @SaCheckPermission("psi:init:confirm")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody InitDocReq req, @PathVariable Long id) {
        initDocService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:init:confirm")
    @DeleteMapping("/{type}/{id}")
    public R<Void> delete(@PathVariable String type, @PathVariable Long id) {
        initDocService.delete(type, id);
        return R.ok();
    }

    @Log(module = "PSI-期初确认")
    @Operation(summary = "确认期初")
    @SaCheckPermission("psi:init:confirm")
    @PostMapping("/{type}/{id}/confirm")
    public R<String> confirm(@PathVariable String type, @PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(initDocService.confirm(type, id, req));
    }
}
