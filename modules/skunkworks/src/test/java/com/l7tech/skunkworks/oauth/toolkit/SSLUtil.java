package com.l7tech.skunkworks.oauth.toolkit;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.PermissiveX509TrustManager;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.util.Pair;

import javax.net.ssl.*;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class SSLUtil {
    /**
     * Use if client cert auth is not required.
     */
    public static SSLSocketFactory getSSLSocketFactory() throws Exception {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{}, new X509TrustManager[]{new PermissiveX509TrustManager()}, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    /**
     * Use if SSL with client cert auth is required.
     */
    public static SSLSocketFactory getSSLSocketFactoryWithKeyManager() throws Exception {
        final Pair<X509Certificate, PrivateKey> certAndKey = TestKeys.getCertAndKey("RSA_1024");
        final SingleCertX509KeyManager keyManager = new SingleCertX509KeyManager(new X509Certificate[]{certAndKey.left}, certAndKey.right);
        final TrustManager trustManager = new PermissiveX509TrustManager();
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{keyManager}, new TrustManager[]{trustManager}, new SecureRandom());
        return sslContext.getSocketFactory();
    }
}
