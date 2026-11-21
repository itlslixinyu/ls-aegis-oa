package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;

@Data
@TableName("psi_todo")
public class PsiTodoDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建人部门（数据权限） */
    private Long deptId;
    private String todoType;
    private String title;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private Boolean done;
}
