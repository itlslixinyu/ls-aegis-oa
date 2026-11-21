package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.biz.psi.enums.MatchStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "银行流水查询")
public class BankStatementQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Query(type = QueryType.EQ)
    private Long accountId;
    @Query(type = QueryType.EQ)
    private MatchStatusEnum matchStatus;
}
