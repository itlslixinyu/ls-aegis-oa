package com.ls.aegis.biz.eoms.controller.repair;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.biz.eoms.domain.RepairMaterial;
import com.ls.aegis.biz.eoms.service.repair.IRepairMaterialService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;

/**
 * 维修工单使用物料明细 Controller
 * 提供维修工单物料使用明细的 CRUD 接口
 *
 * @author EOMS
 */
@RestController
@RequestMapping("/eoms/repair/material")
public class RepairMaterialController
{
    @Autowired
    private IRepairMaterialService repairMaterialService;

    /**
     * 查询维修使用物料明细列表
     */
    @SaCheckPermission(value = {"eoms:settlement:query", "eoms:order:query"}, mode = SaMode.OR)
    @GetMapping("/list")
    public R<PageResp<RepairMaterial>> list(RepairMaterial repairMaterial, @Valid PageQuery pageQuery)
    {
        return R.ok(PageQueryHelper.page(pageQuery, () -> repairMaterialService.selectRepairMaterialList(repairMaterial)));
    }

    /**
     * 获取维修使用物料明细详细信息
     */
    @SaCheckPermission(value = {"eoms:settlement:query", "eoms:order:query"}, mode = SaMode.OR)
    @GetMapping(value = "/{id}")
    public R<RepairMaterial> getInfo(@PathVariable("id") Long id)
    {
        return R.ok(repairMaterialService.selectRepairMaterialById(id));
    }

    /**
     * 新增维修使用物料明细（结算时快照或工程师提交使用明细）
     */
    @Log(module = "维修用料-新增")
    @SaCheckPermission(value = {"eoms:settlement:save", "eoms:order:update", "eoms:order:finish"}, mode = SaMode.OR)
    @PostMapping
    public R<Void> add(@Validated @RequestBody RepairMaterial repairMaterial)
    {
        repairMaterial.setCreateBy(UserContextHolder.getUsername());
        repairMaterialService.insertRepairMaterial(repairMaterial);
        return R.ok();
    }

    /**
     * 修改维修使用物料明细
     */
    @Log(module = "维修用料-修改")
    @SaCheckPermission(value = {"eoms:settlement:save", "eoms:order:update", "eoms:order:finish"}, mode = SaMode.OR)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody RepairMaterial repairMaterial)
    {
        repairMaterial.setUpdateBy(UserContextHolder.getUsername());
        repairMaterialService.updateRepairMaterial(repairMaterial);
        return R.ok();
    }

    /**
     * 删除维修使用物料明细
     */
    @Log(module = "维修用料-删除")
    @SaCheckPermission(value = {"eoms:settlement:save", "eoms:order:update", "eoms:order:finish"}, mode = SaMode.OR)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids)
    {
        repairMaterialService.deleteRepairMaterialByIds(ids);
        return R.ok();
    }
}
