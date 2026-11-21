package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.WarehouseQuery;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.req.WarehouseReq;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.model.resp.WarehouseDetailResp;
import com.ls.aegis.biz.psi.model.resp.WarehouseResp;
import com.ls.aegis.biz.psi.service.WarehouseService;
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
import top.continew.starter.extension.crud.annotation.CrudRequestMapping;
import top.continew.starter.extension.crud.enums.Api;
import top.continew.starter.core.util.validation.ValidationUtils;

@Tag(name = "PSI 仓库 API")
@RestController("psiWarehouseController")
@CrudRequestMapping(value = "/psi/warehouse", api = {Api.PAGE, Api.GET, Api.CREATE, Api.UPDATE, Api.BATCH_DELETE, Api.EXPORT})
public class WarehouseController extends BaseController<WarehouseService, WarehouseResp, WarehouseDetailResp, WarehouseQuery, WarehouseReq> {

    @Operation(summary = "下载仓库导入模板")
    @SaCheckPermission("psi:warehouse:import")
    @GetMapping(value = "/import/template", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) {
        baseService.downloadImportTemplate(response);
    }

    @Operation(summary = "解析仓库导入数据")
    @SaCheckPermission("psi:warehouse:import")
    @PostMapping("/import/parse")
    public PsiImportParseResp parseImport(@RequestPart @NotNull(message = "文件不能为空") MultipartFile file) {
        ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        return baseService.parseImport(file);
    }

    @Operation(summary = "导入仓库数据")
    @SaCheckPermission("psi:warehouse:import")
    @PostMapping("/import")
    public PsiImportResp importData(@RequestBody @Valid PsiImportReq req) {
        return baseService.importData(req);
    }
}
