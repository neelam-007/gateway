package com.l7tech.internal.audit;

import java.security.cert.X509Certificate;

/**
 * Utility class for verifying old exported audit signatures, from 5.2 and 5.3 (pre-5.3.1).
 */
public class AuditRecordCompatilibityVerifier52 {
    private final X509Certificate signerCert;

    public AuditRecordCompatilibityVerifier52(X509Certificate signerCert) {
        this.signerCert = signerCert;
    }

    public boolean verifyAuditRecordSignature(String signature, String parsedRecordInSignableFormat) {
        // TODO implmement bug-for-bug-compatible signature verification
        return false;
    }
}
