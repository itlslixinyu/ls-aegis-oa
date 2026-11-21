package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.biz.sign.domain.SignProject;
import com.ls.aegis.biz.sign.domain.SignRecord;
import com.ls.aegis.biz.sign.domain.SignSeal;
import com.ls.aegis.biz.sign.domain.vo.DashboardStats;
import com.ls.aegis.biz.sign.mapper.SignProjectMapper;
import com.ls.aegis.biz.sign.mapper.SignRecordMapper;
import com.ls.aegis.biz.sign.mapper.SignSealMapper;
import com.ls.aegis.biz.sign.service.ISignDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * 仪表盘服务实现
 */
@Service
public class SignDashboardServiceImpl implements ISignDashboardService {

    @Autowired
    private SignRecordMapper recordMapper;

    @Autowired
    private SignSealMapper sealMapper;

    @Autowired
    private SignProjectMapper projectMapper;

    @Override
    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();

        // 签章项目数
        List<SignProject> projects = projectMapper.selectProjectList(new SignProject());
        stats.setProjectCount(projects != null ? (long) projects.size() : 0L);

        // 印章总数
        List<SignSeal> seals = sealMapper.selectSealList(new SignSeal());
        stats.setSealCount(seals != null ? (long) seals.size() : 0L);

        // 月签章数量
        Long monthCount = recordMapper.countMonthSign();
        stats.setMonthSignCount(monthCount != null ? monthCount : 0L);

        // 授权用户数 - 查询所有不同的操作人
        List<SignRecord> allRecords = recordMapper.selectRecordList(new SignRecord());
        long userCount = allRecords != null ? allRecords.stream()
                .filter(r -> r.getUserName() != null && !r.getUserName().isEmpty())
                .map(SignRecord::getUserName)
                .distinct()
                .count() : 0L;
        stats.setUserCount(userCount);

        // 最近签章
        List<SignRecord> recent = recordMapper.selectRecentRecords(10);
        if (recent != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            List<DashboardStats.RecentSign> recentSigns = recent.stream().map(r -> {
                DashboardStats.RecentSign rs = new DashboardStats.RecentSign();
                rs.setRecordId(r.getRecordId());
                rs.setProjectName(r.getProjectName());
                rs.setSealName(r.getSealName());
                rs.setOperatorName(r.getUserName());
                rs.setSignTime(r.getSignTime() != null ? sdf.format(r.getSignTime()) : "");
                return rs;
            }).toList();
            stats.setRecentSigns(recentSigns);
        }

        // 项目状态分布
        Map<String, Long> statusStats = new HashMap<>();
        if (projects != null) {
            for (SignProject p : projects) {
                String status = p.getStatus() != null ? p.getStatus() : "0";
                statusStats.merge(status, 1L, Long::sum);
            }
        }
        stats.setProjectStatusStats(statusStats);

        // 印章类型分布
        Map<String, Long> categoryStats = new HashMap<>();
        if (seals != null) {
            for (SignSeal s : seals) {
                String category = s.getCategoryLabel();
                if (category == null || category.trim().isEmpty()) {
                    category = s.getSealCategory();
                }
                if (category == null || category.trim().isEmpty()) {
                    category = "其他";
                }
                categoryStats.merge(category, 1L, Long::sum);
            }
        }
        stats.setSealCategoryStats(categoryStats);

        return stats;
    }
}
