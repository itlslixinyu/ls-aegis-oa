package com.ls.aegis.biz.psi.support;

/**
 * 银行账号脱敏（列表展示用；详情仍返回完整账号）
 */
public final class AccountNoMask {

    private AccountNoMask() {
    }

    /**
     * 保留末 4 位；过短则全部打码。
     */
    public static String mask(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return accountNo;
        }
        String raw = accountNo.trim();
        if (raw.length() <= 4) {
            return "****";
        }
        return "*".repeat(Math.min(raw.length() - 4, 12)) + raw.substring(raw.length() - 4);
    }
}
