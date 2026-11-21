package com.ls.aegis.biz.sign.domain.vo;

import java.io.Serializable;
import java.util.List;

/** Mathematical signature integrity only; not certificate trust/revocation validation. */
public record PdfSignatureReport(int signatureCount, boolean allSignaturesIntact,
                                 boolean latestSignatureCoversWholeDocument, boolean continuationAllowed,
                                 String message, String verificationScope, List<Entry> signatures) implements Serializable {
    public record Entry(String fieldName, boolean intact, boolean coversWholeDocument,
                        int revision, String signer, String signedAt) implements Serializable { }
}
