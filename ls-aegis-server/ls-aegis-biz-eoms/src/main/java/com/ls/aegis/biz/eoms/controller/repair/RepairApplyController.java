package com.ls.aegis.biz.eoms.controller.repair;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import top.continew.starter.log.annotation.Log;
import com.ls.aegis.biz.eoms.domain.RepairApply;
import com.ls.aegis.biz.eoms.service.repair.IRepairApplyService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import com.ls.aegis.biz.eoms.model.resp.RepairApplyExportResp;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;

/**
 * 报修申请管理 Controller
 *
 * @author EOMS Generator
 */
@RestController
@RequestMapping("/eoms/repair/apply")
public class RepairApplyController
{
    @Autowired
    private IRepairApplyService repairApplyService;

    /**
     * 获取报修申请列表
     */
    @SaCheckPermission("eoms:repair:list")
    @GetMapping("/list")
    public R<PageResp<RepairApply>> list(RepairApply repairApply, @Valid PageQuery pageQuery)
    {
        return R.ok(PageQueryHelper.page(pageQuery, () -> repairApplyService.selectRepairApplyList(repairApply)));
    }

    /**
     * 导出报修申请
     */
    @SaCheckPermission("eoms:repair:list")
    @Log(module = "报修申请-导出")
    @PostMapping("/export")
    public void export(HttpServletResponse response, RepairApply repairApply)
    {
        PageQueryHelper.startExportLimit();
        List<RepairApply> list = repairApplyService.selectRepairApplyList(repairApply);
        ExcelExportHelper.export(list, RepairApplyExportResp.class, "报修申请数据", response);
    }

    /**
     * 获取报修申请详细信息
     */
    @SaCheckPermission("eoms:repair:query")
    @GetMapping(value = "/{id}")
    public R<RepairApply> getInfo(@PathVariable Long id)
    {
        return R.ok(repairApplyService.selectRepairApplyById(id));
    }

    /**
     * 新增报修申请
     * 自动生成报修编号、默认状态为待处理(WAIT)、默认报修时间为当前时间
     */
    @SaCheckPermission("eoms:repair:add")
    @Log(module = "报修申请-新增")
    @PostMapping
    public R<Void> add(@Validated @RequestBody RepairApply repairApply)
    {
        repairApply.setCreateBy(UserContextHolder.getUsername());
        repairApplyService.insertRepairApply(repairApply);
        return R.ok();
    }

    /**
     * 确认报修（WAIT → CONFIRM）
     */
    @SaCheckPermission("eoms:repair:confirm")
    @Log(module = "报修申请-修改")
    @PutMapping("/confirm/{id}")
    public R<Void> confirm(@PathVariable Long id,
                           @RequestParam(required = false) String remark)
    {
        repairApplyService.confirmApply(id, remark);
        return R.ok();
    }

    /**
     * 取消报修（WAIT → CANCEL）
     * 仅待处理状态可直接取消
     */
    @SaCheckPermission("eoms:repair:cancel")
    @Log(module = "报修申请-修改")
    @PutMapping("/cancel/{id}")
    public R<Void> cancel(@PathVariable Long id,
                          @RequestParam(required = false) String remark)
    {
        repairApplyService.cancelApply(id, remark);
        return R.ok();
    }

    /**
     * 关闭报修（CONFIRM → CANCEL）
     * 已确认状态因故关闭，需填写关闭原因
     */
    @SaCheckPermission("eoms:repair:close")
    @Log(module = "报修申请-修改")
    @PutMapping("/close/{id}")
    public R<Void> close(@PathVariable Long id,
                         @RequestParam(required = false) String remark)
    {
        repairApplyService.closeApply(id, remark);
        return R.ok();
    }

    /**
     * 转工单（CONFIRM → ORDER）
     * 将已确认的报修申请转为维修工单，返回创建的工单ID
     */
    @SaCheckPermission("eoms:repair:assign")
    @Log(module = "报修申请-修改")
    @PutMapping("/toOrder/{id}")
    public R<Long> toOrder(@PathVariable Long id,
                           @RequestParam(required = false) String remark)
    {
        Long orderId = repairApplyService.toOrder(id, remark);
        return R.ok(orderId);
    }

    /**
     * 删除报修申请
     * 级联解除关联工单的 apply_id 引用，再删除报修申请
     */
    @SaCheckPermission("eoms:repair:remove")
    @Log(module = "报修申请-删除")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids)
    {
        repairApplyService.deleteRepairApplyByIds(ids);
        return R.ok();
    }
}
