package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.biz.psi.enums.OpenItemStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "开口台账查询")
public class OpenItemQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "往来单位ID")
    @Query(type = QueryType.EQ)
    private Long unitId;

    @Schema(description = "来源单号")
    @Query(type = QueryType.LIKE)
    private String sourceNo;

    @Schema(description = "状态 OPEN/CLOSED")
    @Query(type = QueryType.EQ)
    private OpenItemStatusEnum docStatus;
}
