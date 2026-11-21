package com.ls.aegis.starter.support.idempotent;

import java.time.Duration;

/**
 * 平台幂等 Redis 键空间（按域隔离，避免现网键冲突）
 */
public final class IdempotentConstants {

    private IdempotentConstants() {
    }

    /** PSI 确认类：{@code psi:idem:{tenant}:{action}:{key}} */
    public static final String PSI_KEY_PREFIX = "psi:idem:";

    /** EOMS 收款令牌：{@code eoms:idem:{tenant}:{action}:{key}} */
    public static final String EOMS_KEY_PREFIX = "eoms:idem:";

    public static final long DEFAULT_TTL_HOURS = 24L;

    /** 收款重试窗口 */
    public static final Duration EOMS_PAYMENT_TTL = Duration.ofMinutes(30);
}
