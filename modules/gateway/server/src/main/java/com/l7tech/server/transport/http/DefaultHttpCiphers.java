package com.l7tech.server.transport.http;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * This class encapsulates the code for initializing the connectors table from the contents of server.xml.
 */
public class DefaultHttpCiphers {
    protected static final Logger logger = Logger.getLogger(DefaultHttpCiphers.class.getName());

    private static final String RECOMMENDED_CIPHERS =
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384," +
                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384," +
                    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA," +
                    "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256," +
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA," +
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA," +
                    "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA," +
                    "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384," +
                    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384," +
                    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA," +
                    "TLS_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_RSA_WITH_AES_256_CBC_SHA256," +
                    "TLS_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA," +
                    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA," +
                    "TLS_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_RSA_WITH_AES_128_CBC_SHA";

    private static final String ALL_VISIBLE_CIPHERS =
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384," +
                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384," +
                    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA," +
                    "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256," +
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA," +
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA," +
                    "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA," +
                    "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384," +
                    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384," +
                    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA," +
                    "TLS_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_RSA_WITH_AES_256_CBC_SHA256," +
                    "TLS_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA," +
                    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA," +
                    "TLS_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_RSA_WITH_AES_128_CBC_SHA";

    @NotNull public static String getRecommendedCiphers() {
        return RECOMMENDED_CIPHERS;
    }

    @NotNull public static String getAllVisibleCiphers() {
        return ALL_VISIBLE_CIPHERS;
    }

    @NotNull private static String[] getJceProviderCiphers(final DefaultKey defaultKeystore) {
        Provider tls10Provider = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_TLS10);
        Provider tls12Provider = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_TLS12);
        return ArrayUtils.union(
                getTestSslContext(defaultKeystore, tls10Provider).getSupportedSSLParameters().getCipherSuites(),
                getTestSslContext(defaultKeystore, tls12Provider).getSupportedSSLParameters().getCipherSuites()
        );
    }

    @NotNull public static String[] getAllSupportedCiphers(final DefaultKey defaultKeystore) {
        return ArrayUtils.union(
                ALL_VISIBLE_CIPHERS.split(","),
                getJceProviderCiphers(defaultKeystore)
        );
    }

    @NotNull public static String[] getAllProtocolVersions(boolean defaultProviderOnly, final DefaultKey defaultKeystore) {
        String[] protos;
        if (defaultProviderOnly) {
            protos = getTestSslContext(defaultKeystore, null).getSupportedSSLParameters().getProtocols();
        } else {
            Provider tls10Provider = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_TLS10);
            Provider tls12Provider = JceProvider.getInstance().getProviderFor(JceProvider.SERVICE_TLS12);
            protos = ArrayUtils.union(
                    getTestSslContext(defaultKeystore, tls10Provider).getSupportedSSLParameters().getProtocols(),
                    getTestSslContext(defaultKeystore, tls12Provider).getSupportedSSLParameters().getProtocols());
        }
        return protos;
    }

    private static SSLContext getTestSslContext(@NotNull DefaultKey defaultKeystore, @Nullable Provider provider) {
        ConcurrentMap<String, SSLContext> testSslContextByProviderName = new ConcurrentHashMap<>();
        final String providerName = provider == null ? "" : provider.getName();
        SSLContext sslContext = testSslContextByProviderName.get(providerName);
        if (sslContext != null)
            return sslContext;
        try {
            final KeyManager[] keyManagers;
            keyManagers = defaultKeystore.getSslKeyManagers();
            sslContext = provider == null ? SSLContext.getInstance("TLS") : SSLContext.getInstance("TLS", provider);
            JceProvider.getInstance().prepareSslContext( sslContext );
            sslContext.init(keyManagers, null, null);
            SSLContext ret = testSslContextByProviderName.putIfAbsent(providerName, sslContext);
            return ret != null ? ret : sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(ExceptionUtils.getMessage(e));
        }
    }
}
