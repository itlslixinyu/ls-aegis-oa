package com.ls.aegis.biz.eoms.mapper.finance;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;
public interface FinanceEventMapper {
    int insertEvent(@Param("type") String type, @Param("id") Long id, @Param("action") String action,
        @Param("relatedId") Long relatedId, @Param("amount") BigDecimal amount,
        @Param("operator") String operator, @Param("reason") String reason);
}
