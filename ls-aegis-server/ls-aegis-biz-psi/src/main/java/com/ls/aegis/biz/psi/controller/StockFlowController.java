package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.StockFlowQuery;
import com.ls.aegis.biz.psi.model.resp.StockFlowExportResp;
import com.ls.aegis.biz.psi.model.resp.StockFlowResp;
import com.ls.aegis.biz.psi.service.StockFlowService;
import com.ls.aegis.biz.psi.support.PsiExportHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;

@Tag(name = "PSI 库存流水 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/stock-flow")
public class StockFlowController {

    private final StockFlowService stockFlowService;

    @SaCheckPermission("psi:stockFlow:list")
    @GetMapping
    public R<PageResp<StockFlowResp>> page(StockFlowQuery query, @Valid PageQuery pageQuery) {
        return R.ok(stockFlowService.page(query, pageQuery));
    }

    @Operation(summary = "导出库存流水")
    @SaCheckPermission("psi:stockFlow:list")
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(StockFlowQuery query, HttpServletResponse response) {
        PsiExportHelper.export(query, stockFlowService::page, StockFlowExportResp.class, "库存流水", response);
    }
}
