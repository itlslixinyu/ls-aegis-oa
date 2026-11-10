package com.ls.aegis.biz.eoms.utils;

/**
 * 图片水印元数据
 *
 * 用于在图片底部烧录符合企业审计要求的完整水印信息：
 * 标题、任务名称、执行人、备注、服务地址、项目名称、时间（标签按阶段区分）。
 *
 * @author EOMS
 */
public class WatermarkInfo
{
    /** 水印标题/品牌名（如"奇瑞展厅"） */
    private String title;

    /** 任务名称（如"单据签署"、"现场勘查"） */
    private String taskName;

    /** 执行人（操作员姓名） */
    private String executor;

    /** 备注信息 */
    private String remark;

    /** 服务地址 */
    private String serviceAddress;

    /** 项目名称 */
    private String projectName;

    /** 时间文案（可为空，默认使用当前时间） */
    private String workTime;

    /**
     * 时间字段标签：报修时间 / 维修时间 / 维修完毕时间 等；
     * 为空时回退为「施工时间」以兼容旧调用。
     */
    private String workTimeLabel;

    /** 经度（可为空） */
    private String longitude;

    /** 纬度（可为空） */
    private String latitude;

    public WatermarkInfo() {}

    /**
     * 便捷构造：仅包含地址和时间的简化水印
     */
    public WatermarkInfo(String title, String serviceAddress, String workTime)
    {
        this.title = title;
        this.serviceAddress = serviceAddress;
        this.workTime = workTime;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTaskName()
    {
        return taskName;
    }

    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }

    public String getExecutor()
    {
        return executor;
    }

    public void setExecutor(String executor)
    {
        this.executor = executor;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getServiceAddress()
    {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress)
    {
        this.serviceAddress = serviceAddress;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getWorkTime()
    {
        return workTime;
    }

    public void setWorkTime(String workTime)
    {
        this.workTime = workTime;
    }

    public String getWorkTimeLabel()
    {
        return workTimeLabel;
    }

    public void setWorkTimeLabel(String workTimeLabel)
    {
        this.workTimeLabel = workTimeLabel;
    }

    public String getLongitude()
    {
        return longitude;
    }

    public void setLongitude(String longitude)
    {
        this.longitude = longitude;
    }

    public String getLatitude()
    {
        return latitude;
    }

    public void setLatitude(String latitude)
    {
        this.latitude = latitude;
    }
}
