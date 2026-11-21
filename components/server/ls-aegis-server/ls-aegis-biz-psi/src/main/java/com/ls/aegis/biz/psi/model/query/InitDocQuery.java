package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InitTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "期初查询")
public class InitDocQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private InitTypeEnum type;
    @Query(type = QueryType.LIKE)
    private String docNo;
    @Query(type = QueryType.EQ)
    private DocStatusEnum docStatus;
}
