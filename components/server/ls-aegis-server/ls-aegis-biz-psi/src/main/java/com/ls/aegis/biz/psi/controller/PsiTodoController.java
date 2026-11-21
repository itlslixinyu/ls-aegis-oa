package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.PsiTodoQuery;
import com.ls.aegis.biz.psi.model.resp.PsiTodoResp;
import com.ls.aegis.biz.psi.service.PsiTodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;

@Tag(name = "PSI 业务待办 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/todo")
public class PsiTodoController {

    private final PsiTodoService psiTodoService;

    @SaCheckPermission("psi:todo:list")
    @GetMapping
    public R<PageResp<PsiTodoResp>> page(PsiTodoQuery query, @Valid PageQuery pageQuery) {
        return R.ok(psiTodoService.page(query, pageQuery));
    }

    @Log(module = "PSI-待办完成")
    @Operation(summary = "标记完成")
    @SaCheckPermission("psi:todo:done")
    @PostMapping("/{id}/done")
    public R<Void> done(@PathVariable Long id) {
        psiTodoService.done(id);
        return R.ok();
    }

    @Log(module = "PSI-待办重开")
    @Operation(summary = "重新打开")
    @SaCheckPermission("psi:todo:done")
    @PostMapping("/{id}/reopen")
    public R<Void> reopen(@PathVariable Long id) {
        psiTodoService.reopen(id);
        return R.ok();
    }
}
