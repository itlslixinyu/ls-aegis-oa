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
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.biz.eoms.domain.RepairRecord;
import com.ls.aegis.biz.eoms.service.repair.IRepairRecordService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;
import com.ls.aegis.biz.eoms.model.resp.RepairRecordExportResp;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;

@RestController
@RequestMapping("/eoms/repair/record")
public class RepairRecordController
{
    @Autowired
    private IRepairRecordService repairRecordService;

    @SaCheckPermission("eoms:record:list")
    @GetMapping("/list")
    public R<PageResp<RepairRecord>> list(RepairRecord repairRecord, @Valid PageQuery pageQuery)
    {
        return R.ok(PageQueryHelper.page(pageQuery, () -> repairRecordService.selectRepairRecordList(repairRecord)));
    }

    @Log(module = "维修记录-导出")
    @SaCheckPermission("eoms:record:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, RepairRecord repairRecord)
    {
        PageQueryHelper.startExportLimit();
        List<RepairRecord> list = repairRecordService.selectRepairRecordList(repairRecord);
        ExcelExportHelper.export(list, RepairRecordExportResp.class, "维修记录管理数据", response);
    }

    @SaCheckPermission("eoms:record:query")
    @GetMapping(value = "/{id}")
    public R<RepairRecord> getInfo(@PathVariable Long id)
    {
        return R.ok(repairRecordService.selectRepairRecordById(id));
    }

    @Log(module = "维修记录-新增")
    @SaCheckPermission("eoms:record:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody RepairRecord repairRecord)
    {
        repairRecord.setCreateBy(UserContextHolder.getUsername());
        repairRecordService.insertRepairRecord(repairRecord);
        return R.ok();
    }

    @Log(module = "维修记录-修改")
    @SaCheckPermission("eoms:record:update")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody RepairRecord repairRecord)
    {
        repairRecord.setUpdateBy(UserContextHolder.getUsername());
        repairRecordService.updateRepairRecord(repairRecord);
        return R.ok();
    }

    @Log(module = "维修记录-删除")
    @SaCheckPermission("eoms:record:delete")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids)
    {
        repairRecordService.deleteRepairRecordByIds(ids);
        return R.ok();
    }
}
