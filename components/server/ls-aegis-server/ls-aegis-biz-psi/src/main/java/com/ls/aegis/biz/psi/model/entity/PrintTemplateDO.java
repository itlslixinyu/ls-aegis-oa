package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;

/**
 * PSI 打印模板
 */
@Data
@TableName("psi_print_template")
public class PrintTemplateDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建人部门（数据权限） */
    private Long deptId;
    private String templateName;
    private String bizType;
    private Long fileId;
    private String contentHtml;
    private String fieldMapping;
    /** 是否默认（0否 1是） */
    private Integer isDefault;
    private DisEnableStatusEnum status;
    private String remark;
}
