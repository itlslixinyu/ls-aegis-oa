package com.ls.aegis.biz.sign.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.biz.sign.domain.vo.DashboardStats;
import com.ls.aegis.biz.sign.service.ISignDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.web.model.R;

/**
 * 仪表盘
 * 【安全强制】经营统计数据绝对禁止匿名访问
 */
@RestController
@RequestMapping("/sign/dashboard")
@SaCheckLogin
public class SignDashboardController {

    @Autowired
    private ISignDashboardService dashboardService;

    /**
     * 获取仪表盘统计数据
     */
    @SaCheckPermission("sign:dashboard:stats")
    @GetMapping("/stats")
    public R<DashboardStats> stats() {
        return R.ok(dashboardService.getStats());
    }
}
