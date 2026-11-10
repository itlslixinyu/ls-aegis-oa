package com.ls.aegis.biz.eoms.quote.bridge;

import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import com.ls.aegis.biz.quote.port.QuoteNoAllocator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 报价编号分配：包装 EomsNoGenerator
 */
@Component
@RequiredArgsConstructor
public class EomsQuoteNoAllocator implements QuoteNoAllocator {

    private final EomsNoGenerator eomsNoGenerator;

    @Override
    public String nextQuoteNo() {
        return eomsNoGenerator.generateQuoteNo();
    }
}
