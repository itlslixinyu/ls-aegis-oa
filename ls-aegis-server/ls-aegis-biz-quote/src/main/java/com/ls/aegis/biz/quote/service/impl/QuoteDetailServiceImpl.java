package com.ls.aegis.biz.quote.service.impl;

import java.util.List;

import com.ls.aegis.biz.quote.domain.QuoteDetail;
import com.ls.aegis.biz.quote.mapper.QuoteDetailMapper;
import com.ls.aegis.biz.quote.service.IQuoteDetailService;
import com.ls.aegis.biz.quote.support.QuoteAccessContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BusinessException;

/**
 * 报价明细 Service 实现
 */
@Service
@RequiredArgsConstructor
public class QuoteDetailServiceImpl implements IQuoteDetailService {

    private final QuoteDetailMapper quoteDetailMapper;

    @Override
    public QuoteDetail selectQuoteDetailById(Long id) {
        return quoteDetailMapper.selectQuoteDetailById(id);
    }

    @Override
    public List<QuoteDetail> selectQuoteDetailList(QuoteDetail quoteDetail) {
        return quoteDetailMapper.selectQuoteDetailList(quoteDetail);
    }

    @Override
    public int insertQuoteDetail(QuoteDetail quoteDetail) {
        throw aggregateWriteRequired();
    }

    @Override
    public int updateQuoteDetail(QuoteDetail quoteDetail) {
        throw aggregateWriteRequired();
    }

    @Override
    public int deleteQuoteDetailByIds(Long[] ids) {
        throw aggregateWriteRequired();
    }

    @Override
    public int deleteQuoteDetailById(Long id) {
        throw aggregateWriteRequired();
    }

    /** No controller/caller uses these legacy mutations. Header locking, permissions and totals are atomic. */
    private BusinessException aggregateWriteRequired() {
        QuoteAccessContext.current();
        return new BusinessException("报价明细必须通过报价单整体保存或删除");
    }
}
