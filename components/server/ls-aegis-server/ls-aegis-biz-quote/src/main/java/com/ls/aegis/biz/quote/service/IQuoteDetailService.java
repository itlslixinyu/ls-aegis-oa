package com.ls.aegis.biz.quote.service;

import java.util.List;

import com.ls.aegis.biz.quote.domain.QuoteDetail;

/**
 * 报价明细 Service
 */
public interface IQuoteDetailService {

    QuoteDetail selectQuoteDetailById(Long id);

    List<QuoteDetail> selectQuoteDetailList(QuoteDetail quoteDetail);

    int insertQuoteDetail(QuoteDetail quoteDetail);

    int updateQuoteDetail(QuoteDetail quoteDetail);

    int deleteQuoteDetailByIds(Long[] ids);

    int deleteQuoteDetailById(Long id);
}
