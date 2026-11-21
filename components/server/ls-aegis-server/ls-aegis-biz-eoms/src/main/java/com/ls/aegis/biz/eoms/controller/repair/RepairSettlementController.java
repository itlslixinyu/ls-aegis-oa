package com.ls.aegis.biz.eoms.controller.repair;

import java.util.List;
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
import com.ls.aegis.biz.eoms.domain.RepairSettlement;
import com.ls.aegis.biz.eoms.service.repair.IRepairSettlementService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;

/**
 * 维修结算管理 Controller
 *
 * @author EOMS Generator
 */
@RestController
@RequestMapping("/eoms/repair/settlement")
public class RepairSettlementController
{
    @Autowired
    private IRepairSettlementService repairSettlementService;

    /**
     * 获取结算单列表
     */
    @SaCheckPermission("eoms:settlement:query")
    @GetMapping("/list")
    public R<PageResp<RepairSettlement>> list(RepairSettlement repairSettlement, @Valid PageQuery pageQuery)
    {
        return R.ok(PageQueryHelper.page(pageQuery, () -> repairSettlementService.selectRepairSettlementList(repairSettlement)));
    }

    /**
     * 获取结算单详细信息
     */
    @SaCheckPermission("eoms:settlement:query")
    @GetMapping(value = "/{id}")
    public R<RepairSettlement> getInfo(@PathVariable Long id)
    {
        return R.ok(repairSettlementService.selectRepairSettlementById(id));
    }

    /**
     * 获取结算单详情（含关联信息和物料明细）
     */
    @SaCheckPermission("eoms:settlement:query")
    @GetMapping(value = "/detail/{id}")
    public R<RepairSettlement> getDetail(@PathVariable Long id)
    {
        RepairSettlement settlement = repairSettlementService.selectRepairSettlementById(id);
        if (settlement == null)
        {
            return R.fail("1", "结算单不存在");
        }
        return R.ok(settlement);
    }

    /**
     * 新增结算单（草稿状态）
     */
    @Log(module = "维修结算-新增")
    @SaCheckPermission("eoms:settlement:save")
    @PostMapping
    public R<Long> add(@Validated @RequestBody RepairSettlement repairSettlement)
    {
        repairSettlement.setCreateBy(UserContextHolder.getUsername());
        int rows = repairSettlementService.insertRepairSettlement(repairSettlement);
        if (rows > 0)
        {
            return R.ok(repairSettlement.getId());
        }
        return R.fail("1", "新增结算单失败");
    }

    /**
     * 修改结算单（仅草稿状态可修改）
     */
    @Log(module = "维修结算-修改")
    @SaCheckPermission("eoms:settlement:save")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody RepairSettlement repairSettlement)
    {
        repairSettlement.setUpdateBy(UserContextHolder.getUsername());
        repairSettlementService.updateRepairSettlement(repairSettlement);
        return R.ok();
    }

    /**
     * 结算确认（8动作原子事务）
     * 1.校验状态 2.计算金额 3.更新结算单 4.更新工单 5.扣减库存+流水
     * 6.生成收入 7.生成成本 8.生成应收账款
     */
    @Log(module = "维修结算-确认")
    @SaCheckPermission("eoms:settlement:confirm")
    @PutMapping("/confirm/{id}")
    public R<Void> confirm(@PathVariable Long id,
                           @RequestParam(required = false) String remark)
    {
        repairSettlementService.confirmSettlement(id, remark);
        return R.ok();
    }

    /**
     * 删除结算单（仅草稿状态可删除）
     */
    @Log(module = "维修结算-删除")
    @SaCheckPermission("eoms:settlement:save")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids)
    {
        repairSettlementService.deleteRepairSettlementByIds(ids);
        return R.ok();
    }
    @Log(module = "维修结算-反确认")
    @SaCheckPermission("eoms:settlement:confirm")
    @PutMapping("/reverse/{id}")
    public R<Void> reverse(@PathVariable Long id, @RequestParam String reason)
    {
        repairSettlementService.reverseSettlement(id, reason);
        return R.ok();
    }

}
