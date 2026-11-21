package com.ls.aegis.biz.eoms.controller.repair;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;
import com.ls.aegis.biz.eoms.model.resp.RepairOrderExportResp;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;

/**
 * 维修工单管理 Controller
 * 
 * @author EOMS Generator
 */
@RestController
@RequestMapping("/eoms/repair/order")
public class RepairOrderController
{
    @Autowired
    private IRepairOrderService repairOrderService;
    @Autowired
    private com.ls.aegis.biz.eoms.service.h5.CustomerConfirmationService customerConfirmationService;

    /**
     * 获取维修工单管理列表
     */
    @SaCheckPermission("eoms:order:query")
    @GetMapping("/list")
    public R<PageResp<RepairOrder>> list(RepairOrder repairOrder, @Valid PageQuery pageQuery)
    {
        return R.ok(PageQueryHelper.page(pageQuery, () -> repairOrderService.selectRepairOrderList(repairOrder)));
    }

    /**
     * 获取工程师角色用户选项（用于工单派单下拉选择）
     * 仅返回具有 eoms_engineer 角色且状态正常的用户
     */
    @SaCheckPermission("eoms:order:query")
    @GetMapping("/engineerOptions")
    public R<?> engineerOptions()
    {
        return R.ok(repairOrderService.selectEngineerOptions());
    }

    /**
     * 导出维修工单管理
     */
    @Log(module = "维修工单-导出")
    @SaCheckPermission("eoms:order:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, RepairOrder repairOrder)
    {
        PageQueryHelper.startExportLimit();
        List<RepairOrder> list = repairOrderService.selectRepairOrderList(repairOrder);
        ExcelExportHelper.export(list, RepairOrderExportResp.class, "维修工单管理数据", response);
    }

    /**
     * 获取维修工单管理详细信息
     */
    @SaCheckPermission("eoms:order:query")
    @GetMapping(value = "/{id}")
    public R<RepairOrder> getInfo(@PathVariable Long id)
    {
        return R.ok(repairOrderService.selectRepairOrderById(id));
    }

    /**
     * 新增维修工单管理
     */
    @Log(module = "维修工单-新增")
    @SaCheckPermission("eoms:order:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody RepairOrder repairOrder)
    {
        repairOrder.setCreateBy(UserContextHolder.getUsername());
        repairOrderService.insertRepairOrder(repairOrder);
        return R.ok();
    }

    /**
     * 修改维修工单管理
     */
    @Log(module = "维修工单-修改")
    @SaCheckPermission("eoms:order:update")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody RepairOrder repairOrder)
    {
        repairOrder.setUpdateBy(UserContextHolder.getUsername());
        repairOrderService.updateRepairOrder(repairOrder);
        return R.ok();
    }

    /**
     * 删除维修工单管理
     */
    @Log(module = "维修工单-删除")
    @SaCheckPermission("eoms:order:delete")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids)
    {
        repairOrderService.deleteRepairOrderByIds(ids);
        return R.ok();
    }

    /**
     * 派单（分配工程师）
     */
    @Log(module = "维修工单-派单")
    @SaCheckPermission("eoms:repair:assign")
    @PutMapping("/assign/{id}")
    public R<Void> assign(@PathVariable Long id,
                          @RequestParam Long engineerId,
                          @RequestParam(required = false) String remark)
    {
        repairOrderService.assignEngineer(id, engineerId, remark);
        return R.ok();
    }

    /**
     * 接单（WAIT_ACCEPT → ACCEPTED）
     */
    @Log(module = "维修工单-接单")
    @SaCheckPermission("eoms:order:accept")
    @PutMapping("/accept/{id}")
    public R<Void> accept(@PathVariable Long id,
                          @RequestParam(required = false) String remark)
    {
        repairOrderService.acceptOrder(id, remark);
        return R.ok();
    }

    /**
     * 开始维修（ACCEPTED → PROCESSING）
     */
    @Log(module = "维修工单-开始维修")
    @SaCheckPermission("eoms:order:start")
    @PutMapping("/start/{id}")
    public R<Void> start(@PathVariable Long id,
                         @RequestParam(required = false) String remark)
    {
        repairOrderService.startProcess(id, remark);
        return R.ok();
    }

    /**
     * 提交结算（PROCESSING → WAIT_SETTLEMENT）
     */
    @Log(module = "维修工单-提交结算")
    @SaCheckPermission("eoms:order:finish")
    @PutMapping("/submit/{id}")
    public R<Void> submit(@PathVariable Long id,
                          @RequestParam(required = false) String remark)
    {
        repairOrderService.submitSettlement(id, remark);
        return R.ok();
    }

    /**
     * 关闭工单（FINISHED → CLOSED）
     */
    @Log(module = "维修工单-关闭")
    @SaCheckPermission("eoms:order:close")
    @PutMapping("/close/{id}")
    public R<Void> close(@PathVariable Long id,
                         @RequestParam(required = false) String remark)
    {
        repairOrderService.closeOrder(id, remark);
        return R.ok();
    }
    /** 反确认后撤销关闭，保留历史，不产生零元结算或账务。 */
    @Log(module = "维修工单-反确认后撤销关闭")
    @SaCheckPermission("eoms:order:close")
    @PutMapping("/cancel/{id}")
    public R<Void> cancel(@PathVariable Long id, @RequestParam String reason)
    {
        repairOrderService.cancelAfterReversal(id, reason);
        return R.ok();
    }

    @SaCheckPermission("eoms:order:query")
    @GetMapping("/customer-confirmation/{id}")
    public R<com.ls.aegis.biz.eoms.model.resp.CustomerConfirmationResp> customerConfirmation(@PathVariable Long id) {
        return R.ok(customerConfirmationService.forStaff(id));
    }

}
