package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.ReceiptQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.ReceiptReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReverseReq;
import com.ls.aegis.biz.psi.model.resp.ReceiptResp;
import com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp;
import com.ls.aegis.biz.psi.service.ReceiptService;
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

import java.util.List;

@Tag(name = "PSI 回款 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/receipt")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final com.ls.aegis.biz.psi.service.impl.CashCompensationService compensationService;

    @Log(module = "PSI-回款作废")
    @SaCheckPermission("psi:receipt:confirm")
    @PostMapping("/{id}/void")
    public R<String> voidCash(@PathVariable Long id, @Valid @RequestBody com.ls.aegis.biz.psi.model.req.CashActionReq req) {
        return R.ok(compensationService.voidCash(true, id, req));
    }

    @Log(module = "PSI-销售退款登记")
    @SaCheckPermission(value = {"psi:receipt:writeoff", "psi:payment:confirm"}, mode = cn.dev33.satoken.annotation.SaMode.AND)
    @PostMapping("/{id}/refund")
    public R<Long> refund(@PathVariable Long id, @Valid @RequestBody com.ls.aegis.biz.psi.model.req.CashRefundReq req) {
        return R.ok(compensationService.refund(true, id, req));
    }

    @Log(module = "PSI-撤销收退款登记")
    @SaCheckPermission(value = {"psi:receipt:confirm", "psi:payment:writeoff"}, mode = cn.dev33.satoken.annotation.SaMode.AND)
    @PostMapping("/{id}/refund/reverse")
    public R<String> reverseRefund(@PathVariable Long id, @Valid @RequestBody com.ls.aegis.biz.psi.model.req.CashActionReq req) {
        return R.ok(compensationService.reverseRefund(true, id, req));
    }

    @SaCheckPermission("psi:receipt:list")
    @GetMapping
    public R<PageResp<ReceiptResp>> page(ReceiptQuery query, @Valid PageQuery pageQuery) {
        return R.ok(receiptService.page(query, pageQuery));
    }

    @Operation(summary = "导出回款")
    @SaCheckPermission("psi:receipt:list")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(ReceiptQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, receiptService::page,
            com.ls.aegis.biz.psi.model.resp.ReceiptExportResp.class, "回款单", response);
    }

    @SaCheckPermission("psi:receipt:list")
    @GetMapping("/{id}")
    public R<ReceiptResp> get(@PathVariable Long id) {
        return R.ok(receiptService.get(id));
    }

    @SaCheckPermission("psi:receipt:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ReceiptReq req) {
        return R.ok(receiptService.create(req));
    }

    @SaCheckPermission("psi:receipt:create")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody ReceiptReq req, @PathVariable Long id) {
        receiptService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:receipt:create")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        receiptService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-回款确认")
    @Operation(summary = "确认回款")
    @SaCheckPermission("psi:receipt:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(receiptService.confirm(id, req));
    }

    @Log(module = "PSI-回款核销")
    @Operation(summary = "核销应收")
    @SaCheckPermission("psi:receipt:writeoff")
    @PostMapping("/{id}/write-off")
    public R<Long> writeOff(@PathVariable Long id, @Valid @RequestBody WriteOffReq req) {
        return R.ok(receiptService.writeOff(id, req));
    }

    @SaCheckPermission("psi:receipt:writeoff")
    @GetMapping("/{id}/write-offs")
    public R<List<WriteOffSummaryResp>> listWriteOffs(@PathVariable Long id) {
        return R.ok(receiptService.listWriteOffs(id));
    }

    @Log(module = "PSI-回款反核销")
    @Operation(summary = "反核销应收")
    @SaCheckPermission("psi:receipt:writeoff")
    @PostMapping("/{id}/reverse")
    public R<Long> reverse(@PathVariable Long id, @Valid @RequestBody WriteOffReverseReq req) {
        return R.ok(receiptService.reverse(id, req));
    }
}
