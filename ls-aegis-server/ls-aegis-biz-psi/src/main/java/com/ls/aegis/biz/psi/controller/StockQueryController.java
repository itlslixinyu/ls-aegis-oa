package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.StockQueryQuery;
import com.ls.aegis.biz.psi.model.resp.StockQueryExportResp;
import com.ls.aegis.biz.psi.model.resp.StockQueryResp;
import com.ls.aegis.biz.psi.service.StockQueryService;
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

/**
 * 库存查询：每个商品每个仓库实时剩余库存
 */
@Tag(name = "PSI 库存查询 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/stock-query")
public class StockQueryController {

    private final StockQueryService stockQueryService;

    @Operation(summary = "分页查询实时库存")
    @SaCheckPermission("psi:stockQuery:list")
    @GetMapping
    public R<PageResp<StockQueryResp>> page(StockQueryQuery query, @Valid PageQuery pageQuery) {
        return R.ok(stockQueryService.page(query, pageQuery));
    }

    @Operation(summary = "导出实时库存")
    @SaCheckPermission("psi:stockQuery:list")
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(StockQueryQuery query, HttpServletResponse response) {
        PsiExportHelper.export(query, stockQueryService::page, StockQueryExportResp.class, "库存查询", response);
    }
}
