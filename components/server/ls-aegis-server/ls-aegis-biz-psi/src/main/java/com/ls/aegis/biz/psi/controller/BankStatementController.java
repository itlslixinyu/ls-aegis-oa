package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.BankStatementQuery;
import com.ls.aegis.biz.psi.model.req.BankImportReq;
import com.ls.aegis.biz.psi.model.req.BankMatchReq;
import com.ls.aegis.biz.psi.model.resp.BankStatementResp;
import com.ls.aegis.biz.psi.model.resp.LabelValueResp;
import com.ls.aegis.biz.psi.service.BankStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;

import java.util.List;

@Tag(name = "PSI 银行对账 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/bank")
public class BankStatementController {

    private final BankStatementService bankStatementService;

    @Log(module = "PSI-银行解除匹配")
    @SaCheckPermission("psi:bank:match")
    @PostMapping("/{id}/unmatch")
    public R<Void> unmatch(@PathVariable Long id, @Valid @RequestBody com.ls.aegis.biz.psi.model.req.CashActionReq req) {
        bankStatementService.unmatch(id, req);
        return R.ok();
    }

    @SaCheckPermission("psi:bank:list")
    @GetMapping
    public R<PageResp<BankStatementResp>> page(BankStatementQuery query, @Valid PageQuery pageQuery) {
        return R.ok(bankStatementService.page(query, pageQuery));
    }

    @SaCheckPermission("psi:bank:list")
    @GetMapping("/{id}")
    public R<BankStatementResp> get(@PathVariable Long id) {
        return R.ok(bankStatementService.get(id));
    }

    @Log(module = "PSI-银行导入")
    @Operation(summary = "导入流水")
    @SaCheckPermission("psi:bank:import")
    @PostMapping("/import")
    public R<Integer> importStatements(@Valid @RequestBody BankImportReq req) {
        return R.ok(bankStatementService.importStatements(req));
    }

    @Operation(summary = "下载银行流水导入模板")
    @SaCheckPermission("psi:bank:import")
    @GetMapping(value = "/import/template", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadImportTemplate(jakarta.servlet.http.HttpServletResponse response) {
        bankStatementService.downloadImportTemplate(response);
    }

    @Operation(summary = "解析银行流水 Excel")
    @SaCheckPermission("psi:bank:import")
    @PostMapping("/import/parse")
    public R<com.ls.aegis.biz.psi.model.resp.BankImportParseResp> parseImport(
        @org.springframework.web.bind.annotation.RequestPart @jakarta.validation.constraints.NotNull(message = "文件不能为空") org.springframework.web.multipart.MultipartFile file) {
        top.continew.starter.core.util.validation.ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        return R.ok(bankStatementService.parseImportExcel(file));
    }

    @Operation(summary = "匹配候选单据")
    @SaCheckPermission("psi:bank:match")
    @GetMapping("/{id}/candidates")
    public R<List<LabelValueResp>> candidates(@PathVariable Long id,
                                              @RequestParam(required = false) String matchBizType) {
        return R.ok(bankStatementService.matchCandidates(id, matchBizType));
    }

    @Log(module = "PSI-银行匹配")
    @Operation(summary = "匹配")
    @SaCheckPermission("psi:bank:match")
    @PostMapping("/match")
    public R<String> match(@Valid @RequestBody BankMatchReq req) {
        return R.ok(bankStatementService.match(req));
    }
}
