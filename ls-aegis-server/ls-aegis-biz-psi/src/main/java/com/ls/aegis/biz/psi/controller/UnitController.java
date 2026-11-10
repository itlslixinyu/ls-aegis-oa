package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.UnitQuery;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.req.UnitReq;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.model.resp.UnitDetailResp;
import com.ls.aegis.biz.psi.model.resp.UnitResp;
import com.ls.aegis.biz.psi.service.UnitService;
import com.ls.aegis.security.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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
 * 往来单位 API
 */
@Tag(name = "PSI 往来单位 API")
@RestController
@RequiredArgsConstructor
@CrudRequestMapping(value = "/psi/unit", api = {Api.PAGE, Api.GET, Api.CREATE, Api.UPDATE, Api.BATCH_DELETE, Api.EXPORT})
public class UnitController extends BaseController<UnitService, UnitResp, UnitDetailResp, UnitQuery, UnitReq> {

    @Operation(summary = "下载往来单位导入模板")
    @SaCheckPermission("psi:unit:import")
    @GetMapping(value = "/import/template", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(HttpServletResponse response) {
        baseService.downloadImportTemplate(response);
    }

    @Operation(summary = "解析往来单位导入数据")
    @SaCheckPermission("psi:unit:import")
    @PostMapping("/import/parse")
    public PsiImportParseResp parseImport(@RequestPart @NotNull(message = "文件不能为空") MultipartFile file) {
        ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        return baseService.parseImport(file);
    }

    @Operation(summary = "导入往来单位数据")
    @SaCheckPermission("psi:unit:import")
    @PostMapping("/import")
    public PsiImportResp importData(@RequestBody @Valid PsiImportReq req) {
        return baseService.importData(req);
    }
}
