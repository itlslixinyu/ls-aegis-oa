package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;

/**
 * 商品分类
 */
@Data
@TableName("psi_goods_category")
public class GoodsCategoryDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建人部门（数据权限） */
    private Long deptId;
    private Long parentId;
    /** 层级编码：一级 10/20/30，二级 1001/2001 */
    private String code;
    private String name;
    private Integer sort;
    private DisEnableStatusEnum status;
}
