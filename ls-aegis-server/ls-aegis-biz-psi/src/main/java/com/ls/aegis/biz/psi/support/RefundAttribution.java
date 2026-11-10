package com.ls.aegis.biz.psi.support;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import top.continew.starter.core.exception.BusinessException;

/** 源现金行锁内的持久序列是因果依据；不能用跨节点时钟或雪花ID猜测资金顺序。 */
public final class RefundAttribution {
    private RefundAttribution() { }
    public enum Kind { ALLOCATE, REVERSE_ALLOCATION, REFUND, REVERSE_REFUND }
    public record Event(Long sequence, Kind kind, BigDecimal amount) { }

    public static BigDecimal available(List<Event> events) {
        BigDecimal allocated = BigDecimal.ZERO, spent = BigDecimal.ZERO, peak = BigDecimal.ZERO;
        for (Event event : events.stream().sorted(Comparator.comparing(Event::sequence, Comparator.nullsFirst(Comparator.naturalOrder()))).toList()) {
            if (event.sequence() == null || event.sequence() <= 0 || event.amount() == null || event.amount().signum() <= 0) {
                throw new BusinessException("历史资金核销缺少可靠顺序或金额，请先对账映射后登记退款");
            }
            switch (event.kind()) {
                case ALLOCATE -> { allocated = allocated.add(event.amount()); peak = peak.max(allocated.add(spent)); }
                case REVERSE_ALLOCATION -> allocated = allocated.subtract(event.amount());
                case REFUND -> spent = spent.add(event.amount());
                case REVERSE_REFUND -> spent = spent.subtract(event.amount());
            }
            if (allocated.signum() < 0 || spent.signum() < 0 || allocated.add(spent).compareTo(peak) > 0) {
                throw new BusinessException("原资金归属与冲销历史不一致，请先对账");
            }
        }
        return peak.subtract(spent).subtract(allocated).max(BigDecimal.ZERO);
    }
}
