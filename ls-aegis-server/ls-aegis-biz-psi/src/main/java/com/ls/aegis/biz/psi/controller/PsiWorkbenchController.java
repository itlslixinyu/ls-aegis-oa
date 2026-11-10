package com.ls.aegis.biz.psi.controller;

import com.ls.aegis.biz.psi.model.resp.PsiWorkbenchSummaryResp;
import com.ls.aegis.biz.psi.service.PsiWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.web.model.R;

@Tag(name = "PSI 工作台 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/workbench")
public class PsiWorkbenchController {

    private final PsiWorkbenchService psiWorkbenchService;

    @Operation(summary = "工作台摘要（仪表盘经营概览）")
    @GetMapping("/summary")
    public R<PsiWorkbenchSummaryResp> summary() {
        return R.ok(psiWorkbenchService.summary());
    }
}
