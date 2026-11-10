package com.ls.aegis.biz.sign.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.sign.domain.SignContractOption;
import com.ls.aegis.biz.sign.domain.SignProject;
import com.ls.aegis.biz.sign.model.resp.SignProjectExportResp;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import com.ls.aegis.biz.sign.service.ISignProjectService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;

import java.util.List;

/**
 * 项目管理
 */
@RestController
@RequestMapping("/sign/project")
public class SignProjectController {

    @Autowired
    private ISignProjectService projectService;

    @SaCheckPermission("sign:project:list")
    @GetMapping("/list")
    public R<PageResp<SignProject>> list(SignProject project, @Valid PageQuery pageQuery) {
        return R.ok(PageQueryHelper.page(pageQuery, () -> projectService.selectProjectList(project)));
    }

    @SaCheckPermission("sign:project:list")
    @GetMapping("/contracts")
    public R<List<SignContractOption>> contracts(@RequestParam(required = false) String contractType,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Long excludeProjectId) {
        return R.ok(projectService.listContractOptions(contractType, keyword, excludeProjectId));
    }

    @Log(module = "项目管理-导出")
    @SaCheckPermission("sign:project:export")
    @GetMapping(value = "/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(HttpServletResponse response, SignProject project) {
        PageQueryHelper.startExportLimit();
        List<SignProject> list = projectService.selectProjectList(project);
        ExcelExportHelper.export(list, SignProjectExportResp.class, "项目数据", response);
    }

    @SaCheckPermission("sign:project:query")
    @GetMapping("/{projectId}")
    public R<SignProject> getInfo(@PathVariable Long projectId) {
        return R.ok(projectService.selectProjectDetail(projectId));
    }

    @Log(module = "项目管理-新增")
    @SaCheckPermission("sign:project:add")
    @PostMapping
    public R<Void> add(@RequestBody SignProject project) {
        projectService.insertProject(project);
        return R.ok();
    }

    @Log(module = "项目管理-修改")
    @SaCheckPermission("sign:project:edit")
    @PutMapping
    public R<Void> edit(@RequestBody SignProject project) {
        projectService.updateProject(project);
        return R.ok();
    }

    @Log(module = "项目管理-删除")
    @SaCheckPermission("sign:project:remove")
    @DeleteMapping("/{projectIds}")
    public R<Void> remove(@PathVariable Long[] projectIds) {
        projectService.deleteProjectByIds(projectIds);
        return R.ok();
    }

    @Log(module = "项目管理-刷新过期")
    @SaCheckPermission("sign:project:edit")
    @PutMapping("/refreshExpired")
    public R<Void> refreshExpired() {
        projectService.refreshExpiredProjects();
        return R.ok();
    }

    @Log(module = "项目审核")
    @SaCheckPermission("sign:project:edit")
    @PutMapping("/review/{projectId}")
    public R<Void> review(@PathVariable Long projectId, @RequestParam String status) {
        projectService.reviewProject(projectId, status);
        return R.ok();
    }
}
