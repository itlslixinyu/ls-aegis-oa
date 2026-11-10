package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "合同查询")
public class ContractQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Query(type = QueryType.LIKE)
    private String contractNo;
    @Query(columns = "contract_name", type = QueryType.LIKE)
    private String name;
    @Query(type = QueryType.EQ)
    private DocStatusEnum docStatus;
    @Query(type = QueryType.EQ)
    private Long unitId;
    @Query(type = QueryType.EQ)
    private Long projectId;
    /** 仅未绑定项目的合同；不走自动 Query */
    private Boolean unboundOnly;
}
