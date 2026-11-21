package com.ls.aegis.biz.sign.domain.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计数据 VO
 */
public class DashboardStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userCount;
    private Long projectCount;
    /** 项目状态分布 { "0": 5, "1": 10, "2": 8 } */
    private Map<String, Long> projectStatusStats;
    private Long sealCount;
    /** 印章类型分布 { "company": 2, "contract": 3 } */
    private Map<String, Long> sealCategoryStats;
    private Long monthSignCount;
    /** 最近签章记录列表 */
    private List<RecentSign> recentSigns;

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public Long getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(Long projectCount) {
        this.projectCount = projectCount;
    }

    public Map<String, Long> getProjectStatusStats() {
        return projectStatusStats;
    }

    public void setProjectStatusStats(Map<String, Long> projectStatusStats) {
        this.projectStatusStats = projectStatusStats;
    }

    public Long getSealCount() {
        return sealCount;
    }

    public void setSealCount(Long sealCount) {
        this.sealCount = sealCount;
    }

    public Map<String, Long> getSealCategoryStats() {
        return sealCategoryStats;
    }

    public void setSealCategoryStats(Map<String, Long> sealCategoryStats) {
        this.sealCategoryStats = sealCategoryStats;
    }

    public Long getMonthSignCount() {
        return monthSignCount;
    }

    public void setMonthSignCount(Long monthSignCount) {
        this.monthSignCount = monthSignCount;
    }

    public List<RecentSign> getRecentSigns() {
        return recentSigns;
    }

    public void setRecentSigns(List<RecentSign> recentSigns) {
        this.recentSigns = recentSigns;
    }

    /**
     * 最近签章记录
     */
    public static class RecentSign implements Serializable {
        private String recordId;
        private String projectName;
        private String sealName;
        private String operatorName;
        private String signTime;

        public String getRecordId() {
            return recordId;
        }

        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getSealName() {
            return sealName;
        }

        public void setSealName(String sealName) {
            this.sealName = sealName;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public String getSignTime() {
            return signTime;
        }

        public void setSignTime(String signTime) {
            this.signTime = signTime;
        }
    }
}
