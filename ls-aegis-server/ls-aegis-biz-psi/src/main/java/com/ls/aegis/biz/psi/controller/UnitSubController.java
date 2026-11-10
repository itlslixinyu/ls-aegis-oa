package com.ls.aegis.biz.psi.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.psi.model.req.UnitAddressReq;
import com.ls.aegis.biz.psi.model.req.UnitContactReq;
import com.ls.aegis.biz.psi.model.req.UnitInvoiceReq;
import com.ls.aegis.biz.psi.model.req.UnitPayReq;
import com.ls.aegis.biz.psi.model.resp.UnitAddressResp;
import com.ls.aegis.biz.psi.model.resp.UnitContactResp;
import com.ls.aegis.biz.psi.model.resp.UnitInvoiceResp;
import com.ls.aegis.biz.psi.model.resp.UnitPayResp;
import com.ls.aegis.biz.psi.service.UnitSubService;
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
import top.continew.starter.web.model.R;

import java.util.List;

/**
 * 往来单位子表 API（联系人 / 开票 / 地址 / 付款账户）
 */
@Tag(name = "PSI 往来单位子表 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/psi/unit/{unitId}")
public class UnitSubController {

    private final UnitSubService unitSubService;

    // ---------- 联系人 ----------

    @Operation(summary = "联系人列表")
    @SaCheckPermission("psi:unit:get")
    @GetMapping("/contacts")
    public R<List<UnitContactResp>> listContacts(@PathVariable Long unitId) {
        return R.ok(unitSubService.listContacts(unitId));
    }

    @Operation(summary = "联系人详情")
    @SaCheckPermission("psi:unit:get")
    @GetMapping("/contacts/{id}")
    public R<UnitContactResp> getContact(@PathVariable Long unitId, @PathVariable Long id) {
        return R.ok(unitSubService.getContact(unitId, id));
    }

    @Operation(summary = "新增联系人")
    @SaCheckPermission("psi:unit:update")
    @PostMapping("/contacts")
    public R<Long> createContact(@PathVariable Long unitId, @Valid @RequestBody UnitContactReq req) {
        return R.ok(unitSubService.createContact(unitId, req));
    }

    @Operation(summary = "更新联系人")
    @SaCheckPermission("psi:unit:update")
    @PutMapping("/contacts/{id}")
    public R<Void> updateContact(@PathVariable Long unitId, @PathVariable Long id, @Valid @RequestBody UnitContactReq req) {
        unitSubService.updateContact(unitId, id, req);
        return R.ok();
    }

    @Operation(summary = "删除联系人")
    @SaCheckPermission("psi:unit:update")
    @DeleteMapping("/contacts/{id}")
    public R<Void> deleteContact(@PathVariable Long unitId, @PathVariable Long id) {
        unitSubService.deleteContact(unitId, id);
        return R.ok();
    }

    // ---------- 开票信息 ----------

    @Operation(summary = "开票信息列表")
    @SaCheckPermission("psi:unit:get")
    @GetMapping("/invoices")
    public R<List<UnitInvoiceResp>> listInvoices(@PathVariable Long unitId) {
        return R.ok(unitSubService.listInvoices(unitId));
    }

    @Operation(summary = "开票信息详情")
    @SaCheckPermission("psi:unit:get")
    @GetMapping("/invoices/{id}")
    public R<UnitInvoiceResp> getInvoice(@PathVariable Long unitId, @PathVariable Long id) {
        return R.ok(unitSubService.getInvoice(unitId, id));
    }

    @Operation(summary = "新增开票信息")
    @SaCheckPermission("psi:unit:update")
    @PostMapping("/invoices")
    public R<Long> createInvoice(@PathVariable Long unitId, @Valid @RequestBody UnitInvoiceReq req) {
        return R.ok(unitSubService.createInvoice(unitId, req));
    }

    @Operation(summary = "更新开票信息")
    @SaCheckPermission("psi:unit:update")
    @PutMapping("/invoices/{id}")
    public R<Void> updateInvoice(@PathVariable Long unitId, @PathVariable Long id, @Valid @RequestBody UnitInvoiceReq req) {
        unitSubService.updateInvoice(unitId, id, req);
        return R.ok();
    }

    @Operation(summary = "删除开票信息")
    @SaCheckPermission("psi:unit:update")
    @DeleteMapping("/invoices/{id}")
    public R<Void> deleteInvoice(@PathVariable Long unitId, @PathVariable Long id) {
        unitSubService.deleteInvoice(unitId, id);
        return R.ok();
    }

    // ---------- 收货地址 ----------

    @Operation(summary = "收货地址列表")
    @SaCheckPermission("psi:unit:get")
    @GetMapping("/addresses")
    public R<List<UnitAddressResp>> listAddresses(@PathVariable Long unitId) {
        return R.ok(unitSubService.listAddresses(unitId));
    }

    @Operation(summary = "收货地址详情")
    @SaCheckPermission("psi:unit:get")
    @GetMapping("/addresses/{id}")
    public R<UnitAddressResp> getAddress(@PathVariable Long unitId, @PathVariable Long id) {
        return R.ok(unitSubService.getAddress(unitId, id));
    }

    @Operation(summary = "新增收货地址")
    @SaCheckPermission("psi:unit:update")
    @PostMapping("/addresses")
    public R<Long> createAddress(@PathVariable Long unitId, @Valid @RequestBody UnitAddressReq req) {
        return R.ok(unitSubService.createAddress(unitId, req));
    }

    @Operation(summary = "更新收货地址")
    @SaCheckPermission("psi:unit:update")
    @PutMapping("/addresses/{id}")
    public R<Void> updateAddress(@PathVariable Long unitId, @PathVariable Long id, @Valid @RequestBody UnitAddressReq req) {
        unitSubService.updateAddress(unitId, id, req);
        return R.ok();
    }

    @Operation(summary = "删除收货地址")
    @SaCheckPermission("psi:unit:update")
    @DeleteMapping("/addresses/{id}")
    public R<Void> deleteAddress(@PathVariable Long unitId, @PathVariable Long id) {
        unitSubService.deleteAddress(unitId, id);
        return R.ok();
    }

    // ---------- 付款账户 ----------

    @Operation(summary = "付款账户列表")
    @SaCheckPermission("psi:unit:get")
    @GetMapping("/pays")
    public R<List<UnitPayResp>> listPays(@PathVariable Long unitId) {
        return R.ok(unitSubService.listPays(unitId));
    }

    @Operation(summary = "付款账户详情")
    @SaCheckPermission("psi:unit:get")
    @GetMapping("/pays/{id}")
    public R<UnitPayResp> getPay(@PathVariable Long unitId, @PathVariable Long id) {
        return R.ok(unitSubService.getPay(unitId, id));
    }

    @Operation(summary = "新增付款账户")
    @SaCheckPermission("psi:unit:update")
    @PostMapping("/pays")
    public R<Long> createPay(@PathVariable Long unitId, @Valid @RequestBody UnitPayReq req) {
        return R.ok(unitSubService.createPay(unitId, req));
    }

    @Operation(summary = "更新付款账户")
    @SaCheckPermission("psi:unit:update")
    @PutMapping("/pays/{id}")
    public R<Void> updatePay(@PathVariable Long unitId, @PathVariable Long id, @Valid @RequestBody UnitPayReq req) {
        unitSubService.updatePay(unitId, id, req);
        return R.ok();
    }

    @Operation(summary = "删除付款账户")
    @SaCheckPermission("psi:unit:update")
    @DeleteMapping("/pays/{id}")
    public R<Void> deletePay(@PathVariable Long unitId, @PathVariable Long id) {
        unitSubService.deletePay(unitId, id);
        return R.ok();
    }
}
