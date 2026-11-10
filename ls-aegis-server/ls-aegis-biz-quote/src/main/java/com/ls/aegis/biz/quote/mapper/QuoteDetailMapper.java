package com.ls.aegis.biz.quote.mapper;

import java.util.List;

import com.ls.aegis.biz.quote.domain.QuoteDetail;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

/**
 * 报价明细 Mapper
 */
public interface QuoteDetailMapper {

    @DataPermission(tableAlias = "q")
    QuoteDetail selectQuoteDetailById(Long id);

    @DataPermission(tableAlias = "q")
    List<QuoteDetail> selectQuoteDetailList(QuoteDetail quoteDetail);

    int insertQuoteDetail(QuoteDetail quoteDetail);

    int updateQuoteDetail(QuoteDetail quoteDetail);

    int deleteQuoteDetailById(Long id);

    int deleteQuoteDetailByIds(Long[] ids);

    int deleteQuoteDetailByQuoteId(Long quoteId);

    int deleteQuoteDetailByQuoteIds(Long[] quoteIds);
}
