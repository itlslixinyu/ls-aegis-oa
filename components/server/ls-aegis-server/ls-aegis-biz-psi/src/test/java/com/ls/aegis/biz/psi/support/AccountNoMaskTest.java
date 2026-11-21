package com.ls.aegis.biz.psi.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AccountNoMaskTest {

    @Test
    void maskKeepsLast4() {
        Assertions.assertEquals("************3456", AccountNoMask.mask("6222021234563456"));
    }

    @Test
    void maskShortBecomesStars() {
        Assertions.assertEquals("****", AccountNoMask.mask("1234"));
        Assertions.assertEquals("****", AccountNoMask.mask("12"));
    }

    @Test
    void maskNullSafe() {
        Assertions.assertNull(AccountNoMask.mask(null));
        Assertions.assertEquals("   ", AccountNoMask.mask("   "));
    }
}
