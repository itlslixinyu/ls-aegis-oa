package com.ls.aegis.biz.eoms.controller.finance;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.biz.eoms.domain.Payment;
import com.ls.aegis.biz.eoms.domain.PaymentVerifyBatchDTO;
import com.ls.aegis.biz.eoms.domain.PaymentVerifyResultVO;
import com.ls.aegis.biz.eoms.service.finance.IPaymentService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;
import com.ls.aegis.biz.eoms.model.resp.EomsReceiptExportResp;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;

/**
 * 维修收款 Controller（表仍为 eoms_payment；路径避开进销存 /psi/payment）
 *
 * @author EOMS Generator
 */
@RestController("eomsPaymentController")
@RequestMapping("/eoms/finance/receipt")
public class PaymentController
{
    @Autowired
    private IPaymentService paymentService;

    /**
     * 获取收款记录列表
     */
    @SaCheckPermission("eoms:payment:query")
    @GetMapping("/list")
    public R<PageResp<Payment>> list(Payment payment, @Valid PageQuery pageQuery)
    {
        return R.ok(PageQueryHelper.page(pageQuery, () -> paymentService.selectPaymentList(payment)));
    }

    /**
     * 导出收款记录
     */
    @SaCheckPermission("eoms:payment:query")
    @PostMapping("/export")
    public void export(HttpServletResponse response, Payment payment)
    {
        PageQueryHelper.startExportLimit();
        List<Payment> list = paymentService.selectPaymentList(payment);
        ExcelExportHelper.export(list, EomsReceiptExportResp.class, "收款记录数据", response);
    }

    /**
     * 获取收款记录详细信息
     */
    @SaCheckPermission("eoms:payment:query")
    @GetMapping(value = "/{id}")
    public R<Payment> getInfo(@PathVariable Long id)
    {
        return R.ok(paymentService.selectPaymentById(id));
    }

    /**
     * 新增收款记录
     */
    @Log(module = "维修收款-新增")
    @SaCheckPermission("eoms:payment:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody Payment payment)
    {
        payment.setCreateBy(UserContextHolder.getUsername());
        paymentService.insertPayment(payment);
        return R.ok();
    }

    /**
     * 修改收款记录
     */
    // P1-1 修复：edit 接口误用 add 权限，导致仅有"新增"权限的角色可越权修改。改为 update。
    @Log(module = "维修收款-修改")
    @SaCheckPermission("eoms:payment:update")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody Payment payment)
    {
        payment.setUpdateBy(UserContextHolder.getUsername());
        paymentService.updatePayment(payment);
        return R.ok();
    }

    /**
     * 删除收款记录
     */
    // P1-1 修复：remove 接口误用 add 权限，导致仅有"新增"权限的角色可越权删除。改为 delete。
    @Log(module = "维修收款-删除")
    @SaCheckPermission("eoms:payment:delete")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids)
    {
        paymentService.deletePaymentByIds(ids);
        return R.ok();
    }

    /**
     * 收款核销（payment → receivable 状态切换）
     * 1.校验应收单状态 2.校验金额 3.插入收款记录
     * 4.原子更新应收单(已收/欠款/状态)
     * 请求体需包含：receivableId、amount、paymentMethod，可选 paymentTime、remark、idempotentToken
     */
    @Log(module = "维修收款-核销")
    @SaCheckPermission("eoms:payment:add")
    @PostMapping("/verify")
    public R<Void> verify(@Validated @RequestBody Payment payment)
    {
        paymentService.verifyReceivable(payment);
        return R.ok();
    }

    /**
     * 批量收款核销（同一客户一次收款核销多笔未结清应收单）
     * <p>
     * 统一收款方式、收款时间、备注；每笔应收单独立核销金额。
     * 单事务内循环核销，任一笔失败全部回滚；整个批量操作共享一个幂等令牌。
     * 返回每笔核销明细及汇总信息，供前端展示核销结果。
     * </p>
     * 请求体需包含：paymentMethod、paymentTime、remark、idempotentToken、items[{receivableId, amount}]
     */
    @Log(module = "维修收款-批量核销")
    @SaCheckPermission("eoms:payment:add")
    @PostMapping("/verify/batch")
    public R<PaymentVerifyResultVO> verifyBatch(@Validated @RequestBody PaymentVerifyBatchDTO dto)
    {
        PaymentVerifyResultVO result = paymentService.verifyReceivableBatch(dto);
        return R.ok(result);
    }
}
