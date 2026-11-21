package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.query.PaymentQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.PaymentReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReverseReq;
import com.ls.aegis.biz.psi.model.resp.PaymentResp;
import com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp;
import com.ls.aegis.biz.psi.service.PaymentService;
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

@Tag(name = "PSI 付款 API")
@RestController("psiPaymentController")
@RequiredArgsConstructor
@RequestMapping("/psi/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final com.ls.aegis.biz.psi.service.impl.CashCompensationService compensationService;

    @Log(module = "PSI-付款作废")
    @SaCheckPermission("psi:payment:confirm")
    @PostMapping("/{id}/void")
    public R<String> voidCash(@PathVariable Long id, @Valid @RequestBody com.ls.aegis.biz.psi.model.req.CashActionReq req) {
        return R.ok(compensationService.voidCash(false, id, req));
    }

    @Log(module = "PSI-采购收退款登记")
    @SaCheckPermission(value = {"psi:payment:writeoff", "psi:receipt:confirm"}, mode = cn.dev33.satoken.annotation.SaMode.AND)
    @PostMapping("/{id}/refund")
    public R<Long> refund(@PathVariable Long id, @Valid @RequestBody com.ls.aegis.biz.psi.model.req.CashRefundReq req) {
        return R.ok(compensationService.refund(false, id, req));
    }

    @Log(module = "PSI-撤销销售退款登记")
    @SaCheckPermission(value = {"psi:payment:confirm", "psi:receipt:writeoff"}, mode = cn.dev33.satoken.annotation.SaMode.AND)
    @PostMapping("/{id}/refund/reverse")
    public R<String> reverseRefund(@PathVariable Long id, @Valid @RequestBody com.ls.aegis.biz.psi.model.req.CashActionReq req) {
        return R.ok(compensationService.reverseRefund(false, id, req));
    }

    @SaCheckPermission("psi:payment:list")
    @GetMapping
    public R<PageResp<PaymentResp>> page(PaymentQuery query, @Valid PageQuery pageQuery) {
        return R.ok(paymentService.page(query, pageQuery));
    }

    @Operation(summary = "导出付款")
    @SaCheckPermission("psi:payment:list")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(PaymentQuery query, jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiExportHelper.export(query, paymentService::page,
            com.ls.aegis.biz.psi.model.resp.PaymentExportResp.class, "付款单", response);
    }

    @SaCheckPermission("psi:payment:list")
    @GetMapping("/{id}")
    public R<PaymentResp> get(@PathVariable Long id) {
        return R.ok(paymentService.get(id));
    }

    @SaCheckPermission("psi:payment:create")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PaymentReq req) {
        return R.ok(paymentService.create(req));
    }

    @SaCheckPermission("psi:payment:create")
    @PutMapping("/{id}")
    public R<Void> update(@Valid @RequestBody PaymentReq req, @PathVariable Long id) {
        paymentService.update(req, id);
        return R.ok();
    }

    @SaCheckPermission("psi:payment:create")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return R.ok();
    }

    @Log(module = "PSI-付款确认")
    @Operation(summary = "确认付款")
    @SaCheckPermission("psi:payment:confirm")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id, @Valid @RequestBody DocConfirmReq req) {
        return R.ok(paymentService.confirm(id, req));
    }

    @Log(module = "PSI-付款核销")
    @Operation(summary = "核销应付")
    @SaCheckPermission("psi:payment:writeoff")
    @PostMapping("/{id}/write-off")
    public R<Long> writeOff(@PathVariable Long id, @Valid @RequestBody WriteOffReq req) {
        return R.ok(paymentService.writeOff(id, req));
    }

    @SaCheckPermission("psi:payment:writeoff")
    @GetMapping("/{id}/write-offs")
    public R<List<WriteOffSummaryResp>> listWriteOffs(@PathVariable Long id) {
        return R.ok(paymentService.listWriteOffs(id));
    }

    @Log(module = "PSI-付款反核销")
    @Operation(summary = "反核销应付")
    @SaCheckPermission("psi:payment:writeoff")
    @PostMapping("/{id}/reverse")
    public R<Long> reverse(@PathVariable Long id, @Valid @RequestBody WriteOffReverseReq req) {
        return R.ok(paymentService.reverse(id, req));
    }
}
