package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.io.PermissiveX509TrustManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public final class SSLUtil {
    // trust everything
    public static SSLSocketFactory getSSLSocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManager[] keyManagers = new KeyManager[]{};
        sslContext.init(keyManagers, new X509TrustManager[]{new PermissiveX509TrustManager()}, null);
        return sslContext.getSocketFactory();
    }
}
