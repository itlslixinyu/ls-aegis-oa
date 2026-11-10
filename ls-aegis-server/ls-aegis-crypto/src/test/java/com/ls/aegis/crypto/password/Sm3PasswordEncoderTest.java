package com.ls.aegis.crypto.password;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sm3PasswordEncoderTest {

    private final Sm3PasswordEncoder encoder = new Sm3PasswordEncoder();

    @Test
    void encodeAndMatch() {
        String encoded = encoder.encode("admin123");
        assertTrue(encoded.startsWith("v1$"));
        assertTrue(encoder.matches("admin123", encoded));
        assertFalse(encoder.matches("wrong", encoded));
    }
}
