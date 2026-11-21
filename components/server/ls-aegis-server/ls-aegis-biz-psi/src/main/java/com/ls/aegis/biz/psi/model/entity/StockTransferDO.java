package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@TableName("psi_stock_transfer")
public class StockTransferDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建人部门（数据权限） */
    private Long deptId;
    private String docNo;
    private DocStatusEnum docStatus;
    private LocalDateTime transferDate;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private String idempotentKey;
    private String remark;
    private Long confirmUser;
    private LocalDateTime confirmTime;
}
