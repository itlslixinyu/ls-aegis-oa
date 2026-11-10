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
import com.ls.aegis.biz.eoms.domain.Income;
import com.ls.aegis.biz.eoms.service.finance.IIncomeService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;
import com.ls.aegis.biz.eoms.model.resp.IncomeExportResp;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;

@RestController
@RequestMapping("/eoms/finance/income")
public class IncomeController
{
    @Autowired
    private IIncomeService incomeService;

    @SaCheckPermission("eoms:income:list")
    @GetMapping("/list")
    public R<PageResp<Income>> list(Income income, @Valid PageQuery pageQuery)
    {
        return R.ok(PageQueryHelper.page(pageQuery, () -> incomeService.selectIncomeList(income)));
    }

    @Log(module = "利润统计-导出")
    @SaCheckPermission("eoms:income:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, Income income)
    {
        PageQueryHelper.startExportLimit();
        List<Income> list = incomeService.selectIncomeList(income);
        ExcelExportHelper.export(list, IncomeExportResp.class, "利润统计管理数据", response);
    }

    @SaCheckPermission("eoms:income:query")
    @GetMapping(value = "/{id}")
    public R<Income> getInfo(@PathVariable Long id)
    {
        return R.ok(incomeService.selectIncomeById(id));
    }

    @Log(module = "利润统计-新增")
    @SaCheckPermission("eoms:income:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody Income income)
    {
        income.setCreateBy(UserContextHolder.getUsername());
        incomeService.insertIncome(income);
        return R.ok();
    }

    @Log(module = "利润统计-修改")
    @SaCheckPermission("eoms:income:update")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody Income income)
    {
        income.setUpdateBy(UserContextHolder.getUsername());
        incomeService.updateIncome(income);
        return R.ok();
    }

    @Log(module = "利润统计-删除")
    @SaCheckPermission("eoms:income:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids)
    {
        incomeService.deleteIncomeByIds(ids);
        return R.ok();
    }
}
