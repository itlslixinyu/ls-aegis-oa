package com.ls.aegis.biz.quote.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

import com.ls.aegis.biz.quote.domain.Quote;
import com.ls.aegis.biz.quote.model.resp.QuoteSummaryResp;

/**
 * 报价 Mapper
 */
public interface QuoteMapper {

    @DataPermission(tableAlias = "q")
    Quote selectQuoteById(Long id);

    /** 同一事务内串行化修改、删除及兼容转工单，避免重复建单和终态覆盖。 */
    @DataPermission(tableAlias = "q")
    Quote selectQuoteByIdForUpdate(Long id);

    @DataPermission(tableAlias = "q")
    List<Quote> selectQuoteList(Quote quote);

    @DataPermission(tableAlias = "q")
    QuoteSummaryResp selectQuoteSummary(@Param("monthStart") String monthStart);

    int insertQuote(Quote quote);

    @DataPermission(tableAlias = "q")
    int updateQuote(Quote quote);

    @DataPermission(tableAlias = "q")
    int deleteQuoteById(Long id);

    @DataPermission(tableAlias = "q")
    int deleteQuoteByIds(Long[] ids);
}
