package com.ls.aegis.biz.psi.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "业务待办查询")
public class PsiTodoQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Query(type = QueryType.EQ)
    private String todoType;

    @Query(type = QueryType.EQ)
    private String bizType;

    @Query(type = QueryType.EQ)
    private Boolean done;

    @Query(type = QueryType.LIKE)
    private String title;
}
