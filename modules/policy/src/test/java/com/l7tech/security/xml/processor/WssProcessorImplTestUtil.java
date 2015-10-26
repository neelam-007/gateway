package com.l7tech.security.xml.processor;

/**
 * Allows tests to work with test data that has expired, so we don't have to keep pointlessly regenerating them.
 */
public class WssProcessorImplTestUtil {

    public static void restoreDefaults() {
        WssProcessorImpl.checkSigningCertExpiry = WssProcessorImpl.DEFAULT_CHECK_SIGNING_CERT_EXPIRY;
    }

    public static void disableSigningCertExpiryCheck() {
        WssProcessorImpl.checkSigningCertExpiry = false;
    }
}
