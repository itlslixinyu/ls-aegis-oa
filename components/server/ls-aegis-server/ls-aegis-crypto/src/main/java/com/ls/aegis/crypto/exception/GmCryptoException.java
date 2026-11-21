package com.ls.aegis.crypto.exception;

/**
 * 国密运算异常。
 */
public class GmCryptoException extends RuntimeException {

    public GmCryptoException(String message) {
        super(message);
    }

    public GmCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
