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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.biz.eoms.domain.Receivable;
import com.ls.aegis.biz.eoms.service.finance.IReceivableService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;
import com.ls.aegis.biz.eoms.model.resp.ReceivableExportResp;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;

/**
 * 维修应收 Controller（表仍为 eoms_receivable）
 * 
 * @author EOMS Generator
 */
@RestController
@RequestMapping("/eoms/finance/receivable")
public class ReceivableController
{
    @Autowired
    private IReceivableService receivableService;

    /**
     * 获取应收账款管理列表
     */
    @SaCheckPermission("eoms:receivable:query")
    @GetMapping("/list")
    public R<PageResp<Receivable>> list(Receivable receivable, @Valid PageQuery pageQuery)
    {
        return R.ok(PageQueryHelper.page(pageQuery, () -> receivableService.selectReceivableList(receivable)));
    }

    /**
     * 查询指定客户的未结清应收单（UNPAID 或 PARTIAL）
     * 用于收款登记页面按客户加载待核销的应收单列表
     */
    @SaCheckPermission("eoms:payment:query")
    @GetMapping("/unsettled")
    public R<List<Receivable>> unsettled(@RequestParam Long customerId)
    {
        if (customerId == null)
        {
            throw new ServiceException("客户ID不能为空");
        }
        List<Receivable> list = receivableService.selectUnsettledByCustomerId(customerId);
        return R.ok(list);
    }

    /**
     * 导出应收账款管理
     */
    @Log(module = "维修应收-导出")
    @SaCheckPermission("eoms:receivable:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, Receivable receivable)
    {
        PageQueryHelper.startExportLimit();
        List<Receivable> list = receivableService.selectReceivableList(receivable);
        ExcelExportHelper.export(list, ReceivableExportResp.class, "应收账款管理数据", response);
    }

    /**
     * 获取应收账款管理详细信息
     */
    @SaCheckPermission("eoms:receivable:query")
    @GetMapping(value = "/{id}")
    public R<Receivable> getInfo(@PathVariable Long id)
    {
        return R.ok(receivableService.selectReceivableById(id));
    }

    /**
     * 新增应收账款管理
     */
    @Log(module = "维修应收-新增")
    @SaCheckPermission("eoms:receivable:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody Receivable receivable)
    {
        receivable.setCreateBy(UserContextHolder.getUsername());
        receivableService.insertReceivable(receivable);
        return R.ok();
    }

    /**
     * 修改应收账款管理
     */
    @Log(module = "维修应收-修改")
    @SaCheckPermission("eoms:receivable:update")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody Receivable receivable)
    {
        receivable.setUpdateBy(UserContextHolder.getUsername());
        receivableService.updateReceivable(receivable);
        return R.ok();
    }

    /**
     * 删除应收账款管理
     */
    @Log(module = "维修应收-删除")
    @SaCheckPermission("eoms:receivable:delete")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids)
    {
        receivableService.deleteReceivableByIds(ids);
        return R.ok();
    }
}
