package com.l7tech.server.log.syslog.impl;

import com.l7tech.server.security.keystore.SsgKeyStoreManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509TrustManager;

/**
 * User: vchan
 */
public class SyslogSslClientSupport {

    private static X509TrustManager trustManager;

    private static KeyManager[] defaultKeyManagers;

    private static SsgKeyStoreManager ssgKeyStoreManager;

    static boolean isInitialized() {
        return (trustManager != null && defaultKeyManagers != null && ssgKeyStoreManager != null);
    }

    static X509TrustManager getTrustManager() {
        return trustManager;
    }

    static void setTrustManager(X509TrustManager trustManager) {
        SyslogSslClientSupport.trustManager = trustManager;
    }

    static KeyManager[] getDefaultKeyManagers() {
        return defaultKeyManagers;
    }

    static void setDefaultKeyManagers(KeyManager[] defaultKeyManagers) {
        SyslogSslClientSupport.defaultKeyManagers = defaultKeyManagers;
    }

    static SsgKeyStoreManager getSsgKeyStoreManager() {
        return ssgKeyStoreManager;
    }

    static void setSsgKeyStoreManager(SsgKeyStoreManager ssgKeyStoreManager) {
        SyslogSslClientSupport.ssgKeyStoreManager = ssgKeyStoreManager;
    }
}
