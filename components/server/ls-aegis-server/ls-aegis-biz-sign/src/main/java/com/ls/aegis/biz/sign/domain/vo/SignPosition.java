package com.ls.aegis.biz.sign.domain.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;

import java.io.Serializable;

/**
 * 签章位置 VO — 对应前端 SealStamp 对象
 *
 * <h3>字段说明</h3>
 * <p><b>【标准字段】</b>推荐使用，前后端统一：</p>
 * <ul>
 *   <li>page - 页码</li>
 *   <li>x, y - 坐标（pt）</li>
 *   <li>size - 印章边长（pt），时间戳类型为 0</li>
 *   <li>signMode - 签章模式</li>
 *   <li>sealName - 印章名称</li>
 *   <li>sealFilePath - 印章文件路径</li>
 *   <li>isTimestampOnly - 是否纯时间戳</li>
 *   <li>hasTimestamp - 是否附带时间戳</li>
 *   <li>timestamp - 时间戳文本</li>
 * </ul>
 * <p><b>【坐标约束】</b>所有几何参数做范围硬限制，防止极大值导致 PDF 渲染崩溃。
 * 目前上限 14400pt ≈ 5.08m（理论 A0 纸对角线不到 2m），足够正常业务使用。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignPosition implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 坐标/尺寸最大值（pt）— 约等于 5m，防止越界值 */
    private static final float MAX_COORD = 14400f;
    /** 印章尺寸最大值（pt）— 2000pt ≈ 70cm */
    private static final float MAX_SIZE = 2000f;
    /** PDF 页码最大值（防止传入 Integer.MAX_VALUE 导致遍历爆内存） */
    private static final int MAX_PAGE = 100000;

    /** PDF 页码（从1开始，必填） */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须 ≥ 1")
    @Max(value = MAX_PAGE, message = "页码过大，最大支持 " + MAX_PAGE)
    private Integer page;

    /** 左上角 X 坐标（pt），必填，≥ 0 */
    @NotNull(message = "X 坐标不能为空")
    @DecimalMin(value = "0.0", message = "X 坐标不能为负数")
    @DecimalMax(value = "" + MAX_COORD, message = "X 坐标超出上限 " + MAX_COORD + "pt")
    private Float x;

    /** 左上角 Y 坐标（pt，前端 Y 向下），必填，≥ 0 */
    @NotNull(message = "Y 坐标不能为空")
    @DecimalMin(value = "0.0", message = "Y 坐标不能为负数")
    @DecimalMax(value = "" + MAX_COORD, message = "Y 坐标超出上限 " + MAX_COORD + "pt")
    private Float y;

    /**
     * 印章边长（pt），时间戳类型为 0。
     * <p>这是标准字段，width/height 是兼容别名，实际都指向 size。</p>
     */
    @DecimalMin(value = "0.0", message = "印章尺寸不能为负数")
    @DecimalMax(value = "" + MAX_SIZE, message = "印章尺寸超出上限 " + MAX_SIZE + "pt")
    private Float size;

    /**
     * 签章模式：single/fixed/custom/span
     * <p>兼容别名：sign_mode, mode</p>
     */
    @JsonAlias({"sign_mode", "mode"})
    @Pattern(regexp = "^(single|fixed|custom|span|multi|continuous)?$",
             message = "签章位置模式非法")
    private String signMode;

    /**
     * 印章名称（用于日志和区分）
     * <p>兼容别名：seal_name, name</p>
     */
    @Size(max = 100, message = "印章名称长度不能超过100字符")
    @JsonAlias({"seal_name", "name"})
    private String sealName;

    /**
     * 印章文件相对路径，用于后端从 MinIO 加载图片
     * <p>兼容别名：seal_file_path, imageUrl</p>
     */
    @Size(max = 500, message = "印章路径长度不能超过500字符")
    @JsonAlias({"seal_file_path", "imageUrl"})
    private String sealFilePath;

    // --- 骑缝章专用 ---

    /** 骑缝章总份数，>0 表示骑缝模式 */
    @Min(value = 0, message = "骑缝章总份数不能为负数")
    @Max(value = 500, message = "骑缝章总份数超过合理上限 500")
    @JsonAlias("span_total")
    private Integer spanTotal;

    /** 骑缝章当前片索引（从0开始） */
    @Min(value = 0, message = "骑缝章片索引不能为负数")
    @Max(value = 500, message = "骑缝章片索引超过合理上限 500")
    @JsonAlias("span_index")
    private Integer spanIndex;

    /** 骑缝章组 ID（用于前端联动拖拽，后端仅需回传） */
    @Size(max = 64, message = "spanGroupId 长度不能超过64")
    @JsonAlias("span_group_id")
    private String spanGroupId;

    /** 骑缝章位置（left/right）— 兼容旧版前端 */
    @Pattern(regexp = "^(left|right)?$", message = "骑缝章位置仅允许 left/right")
    @JsonAlias("span_position")
    private String spanPosition;

    // --- 时间戳专用 ---

    /** 是否附带时间戳标签（接受 "0"/"1"/"true"/"false"） */
    @JsonDeserialize(using = FlexibleBooleanDeserializer.class)
    @JsonAlias("has_timestamp")
    private Boolean hasTimestamp;

    /** 时间戳文本（长度限制避免前端注入超长文本写入 PDF） */
    @Size(max = 200, message = "时间戳文本长度不能超过200字符")
    private String timestamp;

    /**
     * 是否为纯时间戳（无印章图片）
     * <p>标准字段，兼容别名：is_timestamp_only, isTimestamp</p>
     */
    @JsonDeserialize(using = FlexibleBooleanDeserializer.class)
    @JsonAlias({"is_timestamp_only", "isTimestamp"})
    private Boolean isTimestampOnly;

    // ====== getters/setters ======

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Float getX() { return x; }
    public void setX(Float x) { this.x = x; }

    public Float getY() { return y; }
    public void setY(Float y) { this.y = y; }

    public Float getSize() { return size; }
    public void setSize(Float size) { this.size = size; }

    public String getSignMode() { return signMode; }
    public void setSignMode(String signMode) { this.signMode = signMode; }

    public String getSealName() { return sealName; }
    public void setSealName(String sealName) { this.sealName = sealName; }

    public String getSealFilePath() { return sealFilePath; }
    public void setSealFilePath(String sealFilePath) { this.sealFilePath = sealFilePath; }

    public Integer getSpanTotal() { return spanTotal; }
    public void setSpanTotal(Integer spanTotal) { this.spanTotal = spanTotal; }

    public Integer getSpanIndex() { return spanIndex; }
    public void setSpanIndex(Integer spanIndex) { this.spanIndex = spanIndex; }

    public String getSpanGroupId() { return spanGroupId; }
    public void setSpanGroupId(String spanGroupId) { this.spanGroupId = spanGroupId; }

    public String getSpanPosition() { return spanPosition; }
    public void setSpanPosition(String s) { this.spanPosition = s; }

    public Boolean getHasTimestamp() { return hasTimestamp; }
    public void setHasTimestamp(Boolean hasTimestamp) { this.hasTimestamp = hasTimestamp; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Boolean getIsTimestampOnly() { return isTimestampOnly; }
    public void setIsTimestampOnly(Boolean isTimestampOnly) { this.isTimestampOnly = isTimestampOnly; }

    // ========== 兼容方法（旧字段 → 标准字段） ==========

    /**
     * @deprecated 兼容旧字段，请使用 getSize() 代替
     */
    @Deprecated
    public Float getWidth() { return size; }

    /**
     * @deprecated 兼容旧字段，请使用 setSize() 代替
     */
    @Deprecated
    public void setWidth(Float w) { this.size = w; }

    /**
     * @deprecated 兼容旧字段，请使用 getSize() 代替
     */
    @Deprecated
    public Float getHeight() { return size; }

    /**
     * @deprecated 兼容旧字段，请使用 setSize() 代替
     */
    @Deprecated
    public void setHeight(Float h) { this.size = h; }

    /**
     * @deprecated 兼容旧字段，请使用 getIsTimestampOnly() 代替
     */
    @Deprecated
    public Boolean getIsTimestamp() { return isTimestampOnly; }

    /**
     * @deprecated 兼容旧字段，请使用 setIsTimestampOnly() 代替
     */
    @Deprecated
    @JsonSetter("isTimestamp")
    public void setIsTimestamp(Object v) { this.isTimestampOnly = FlexibleBooleanDeserializer.parse(v); }
}
