package com.ls.aegis.biz.eoms.controller.contract;

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
import com.ls.aegis.biz.eoms.domain.Contract;
import com.ls.aegis.biz.eoms.service.contract.IContractService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;
import com.ls.aegis.biz.eoms.model.resp.EomsContractExportResp;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;

/**
 * 合同管理 Controller
 * 
 * @author EOMS Generator
 */
@RestController
@RequestMapping("/eoms/contract")
public class ContractController
{
    @Autowired
    private IContractService contractService;

    /**
     * 获取合同管理列表
     */
    @SaCheckPermission("eoms:contract:list")
    @GetMapping("/list")
    public R<PageResp<Contract>> list(Contract contract, @Valid PageQuery pageQuery)
    {
        return R.ok(PageQueryHelper.page(pageQuery, () -> contractService.selectContractList(contract)));
    }

    @Log(module = "合同管理-导出")
    @SaCheckPermission("eoms:contract:export")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(HttpServletResponse response, Contract contract)
    {
        PageQueryHelper.startExportLimit();
        List<Contract> list = contractService.selectContractList(contract);
        ExcelExportHelper.export(list, EomsContractExportResp.class, "合同管理数据", response);
    }

    /**
     * 获取合同管理详细信息
     */
    @SaCheckPermission("eoms:contract:query")
    @GetMapping(value = "/{id}")
    public R<Contract> getInfo(@PathVariable Long id)
    {
        return R.ok(contractService.selectContractById(id));
    }

    /**
     * 新增合同管理
     */
    @Log(module = "合同管理-新增")
    @SaCheckPermission("eoms:contract:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody Contract contract)
    {
        contract.setCreateBy(UserContextHolder.getUsername());
        contractService.insertContract(contract);
        return R.ok();
    }

    /**
     * 修改合同管理
     */
    @Log(module = "合同管理-修改")
    @SaCheckPermission("eoms:contract:update")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody Contract contract)
    {
        contract.setUpdateBy(UserContextHolder.getUsername());
        contractService.updateContract(contract);
        return R.ok();
    }

    /**
     * 删除合同管理
     */
    @Log(module = "合同管理-删除")
    @SaCheckPermission("eoms:contract:delete")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids)
    {
        contractService.deleteContractByIds(ids);
        return R.ok();
    }

    /**
     * 确认生效
     */
    @Log(module = "合同管理-确认")
    @SaCheckPermission("eoms:contract:update")
    @PostMapping("/{id}/confirm")
    public R<String> confirm(@PathVariable Long id)
    {
        return R.ok(contractService.confirm(id));
    }

    /**
     * 绑定/解绑所属项目
     */
    @SaCheckPermission("eoms:contract:update")
    @PutMapping("/{id}/project")
    public R<Void> bindProject(@PathVariable Long id, @RequestBody(required = false) Contract body)
    {
        Long projectId = body == null ? null : body.getProjectId();
        int rows = contractService.bindProject(id, projectId);
        if (rows <= 0)
        {
            return R.fail("1", "合同不存在");
        }
        return R.ok();
    }
}
