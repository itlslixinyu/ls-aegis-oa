package com.ls.aegis.biz.sign.domain.vo;

import java.io.Serializable;

public record SignaturePreservationResult(int beforeCount, int afterCount, int addedCount,
                                          boolean previousSignaturesPreserved, boolean integrityVerified,
                                          String verificationScope) implements Serializable { }
