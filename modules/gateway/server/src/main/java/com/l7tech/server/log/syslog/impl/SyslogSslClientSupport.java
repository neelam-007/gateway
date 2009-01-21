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

    public static boolean isInitialized() {
        return (trustManager != null && defaultKeyManagers != null && ssgKeyStoreManager != null);
    }

    public static X509TrustManager getTrustManager() {
        return trustManager;
    }

    public static void setTrustManager(X509TrustManager trustManager) {
        SyslogSslClientSupport.trustManager = trustManager;
    }

    public static KeyManager[] getDefaultKeyManagers() {
        return defaultKeyManagers;
    }

    public static void setDefaultKeyManagers(KeyManager[] defaultKeyManagers) {
        SyslogSslClientSupport.defaultKeyManagers = defaultKeyManagers;
    }

    public static SsgKeyStoreManager getSsgKeyStoreManager() {
        return ssgKeyStoreManager;
    }

    public static void setSsgKeyStoreManager(SsgKeyStoreManager ssgKeyStoreManager) {
        SyslogSslClientSupport.ssgKeyStoreManager = ssgKeyStoreManager;
    }
}
