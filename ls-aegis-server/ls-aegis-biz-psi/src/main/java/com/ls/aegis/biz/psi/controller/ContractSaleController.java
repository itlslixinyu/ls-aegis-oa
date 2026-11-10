package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.ContractQuery;
import com.ls.aegis.biz.psi.model.req.BindProjectReq;
import com.ls.aegis.biz.psi.model.req.ContractReq;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.resp.ContractResp;
import com.ls.aegis.biz.psi.service.ContractSaleService;
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

@Tag(name = "PSI 销售合同 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/contract-sale")
public class ContractSaleController {

    private final ContractSaleService contractSaleService;

    @SaCheckPermission("psi:contractSale:list")
    @GetMapping
    public R<PageResp<ContractResp>> page(ContractQuery query, @Valid PageQuery pageQuery) {
        return R.ok(contractSaleService.page(query, pageQuery));
    }

    @Operation(summary = "导出销售合同")
    @SaCheckPermission("psi:contractSale:list")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(ContractQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, contractSaleService::page,
            com.ls.aegis.biz.psi.model.resp.ContractExportResp.class, "销售合同", response);
    }

    @SaCheckPermission("psi:contractSale:list")
    @GetMapping("/{id}")
    public R<ContractResp> get(@PathVariable Long id) {
        return R.ok(contractSaleService.get(id));
    }

    @SaCheckPermission("psi:contractSale:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ContractReq req) {
        return R.ok(contractSaleService.create(req));
    }

    @SaCheckPermission("psi:contractSale:update")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody ContractReq req, @PathVariable Long id) {
        contractSaleService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:contractSale:update")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contractSaleService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-销售合同确认")
    @SaCheckPermission("psi:contractSale:update")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(contractSaleService.confirm(id, req));
    }

    @Operation(summary = "执行进度")
    @SaCheckPermission("psi:contractSale:list")
    @GetMapping("/{id}/progress")
    public R<ContractResp> progress(@PathVariable Long id) {
        return R.ok(contractSaleService.progress(id));
    }

    @Log(module = "PSI-销售合同绑定项目")
    @SaCheckPermission("psi:contractSale:update")
    @PutMapping("/{id}/project")
    public R<Void> bindProject(@PathVariable Long id, @RequestBody BindProjectReq req) {
        contractSaleService.bindProject(id, req == null ? null : req.getProjectId());
        return R.ok();
    }
}
