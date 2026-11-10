package com.ls.aegis.biz.quote.port;

/**
 * 报价编号分配端口（由运维报修等模块实现；禁止 biz-quote 反向依赖 eoms）
 */
public interface QuoteNoAllocator {

    /**
     * 生成下一报价单编号（前缀 + yyMMdd + 两位日流水，如 BJ26081901）
     *
     * @return 报价编号
     */
    String nextQuoteNo();
}
