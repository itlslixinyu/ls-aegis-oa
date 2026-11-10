package com.ls.aegis.mybatis.base.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * XML Mapper 存量实体基类（C4）。
 * <p>
 * 继承 {@link BaseDO}。G1 不改业务表：仍用 {@code create_by}/{@code update_by}（用户名），
 * 无 {@code tenant_id}/{@code deleted} 列，故查询 {@code params}/{@code searchValue} 与用户名审计字段保留在此。
 * Sign / Quote / EOMS 共用；表结构对齐后再切 {@link TenantBaseDO}。
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"createUser", "updateUser", "deleted"})
public class XmlMapperDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 搜索值（非表字段） */
    @JsonIgnore
    @TableField(exist = false)
    private String searchValue;

    /** 表字段 create_by，用户名 */
    private String createBy;

    /** 表字段 update_by，用户名 */
    private String updateBy;

    /** 备注 */
    private String remark;

    /** 请求参数（非表字段，如 params.beginTime） */
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> params;

    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }

    @Override
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime getCreateTime() {
        return super.getCreateTime();
    }

    @Override
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(LocalDateTime createTime) {
        super.setCreateTime(createTime);
    }

    @Override
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime getUpdateTime() {
        return super.getUpdateTime();
    }

    @Override
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUpdateTime(LocalDateTime updateTime) {
        super.setUpdateTime(updateTime);
    }

    /**
     * {@link Date} 转 {@link LocalDateTime}，给仍用 Date 的业务时刻（签章时间等）填审计字段。
     */
    public static LocalDateTime from(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
