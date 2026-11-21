package com.ls.aegis.biz.quote.service;

import java.util.List;

import com.ls.aegis.biz.quote.domain.Quote;
import com.ls.aegis.biz.quote.model.resp.QuoteSummaryResp;

/**
 * 报价 Service
 */
public interface IQuoteService {

    Quote selectQuoteById(Long id);

    List<Quote> selectQuoteList(Quote quote);

    /** 工作总览摘要：草稿数 / 本月笔数 / 本月金额 */
    QuoteSummaryResp selectQuoteSummary();

    int insertQuote(Quote quote);

    int updateQuote(Quote quote);

    int deleteQuoteByIds(Long[] ids);

    int deleteQuoteById(Long id);

    /**
     * 报价单转工单（原子事务）
     *
     * @param quoteId 报价单 ID
     * @param address 维修地址（可选）
     * @param remark  备注（可选）
     * @return 新创建的维修工单 ID
     */
    Long convertToOrder(Long quoteId, String address, String remark);
}
