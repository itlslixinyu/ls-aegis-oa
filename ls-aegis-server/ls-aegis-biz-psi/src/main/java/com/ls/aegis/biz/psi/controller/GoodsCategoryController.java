package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.GoodsCategoryQuery;
import com.ls.aegis.biz.psi.model.req.GoodsCategoryReq;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.resp.GoodsCategoryResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.service.GoodsCategoryService;
import com.ls.aegis.security.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.util.validation.ValidationUtils;
import top.continew.starter.extension.crud.annotation.CrudRequestMapping;
import top.continew.starter.extension.crud.enums.Api;

/**
 * PSI 商品分类 API
 */
@Tag(name = "PSI 商品分类 API")
@RestController
@CrudRequestMapping(value = "/psi/goods-category", api = {Api.TREE, Api.GET, Api.CREATE, Api.UPDATE, Api.BATCH_DELETE})
public class GoodsCategoryController extends BaseController<GoodsCategoryService, GoodsCategoryResp, GoodsCategoryResp, GoodsCategoryQuery, GoodsCategoryReq> {

    @Operation(summary = "导出商品分类")
    @SaCheckPermission("psi:goodsCategory:export")
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(GoodsCategoryQuery query, HttpServletResponse response) {
        baseService.export(query, response);
    }

    @Operation(summary = "下载商品分类导入模板")
    @SaCheckPermission("psi:goodsCategory:import")
    @GetMapping(value = "/import/template", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) {
        baseService.downloadImportTemplate(response);
    }

    @Operation(summary = "解析商品分类导入数据")
    @SaCheckPermission("psi:goodsCategory:import")
    @PostMapping("/import/parse")
    public PsiImportParseResp parseImport(@RequestPart @NotNull(message = "文件不能为空") MultipartFile file) {
        ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        return baseService.parseImport(file);
    }

    @Operation(summary = "导入商品分类")
    @SaCheckPermission("psi:goodsCategory:import")
    @PostMapping("/import")
    public PsiImportResp importData(@RequestBody @Valid PsiImportReq req) {
        return baseService.importData(req);
    }
}
