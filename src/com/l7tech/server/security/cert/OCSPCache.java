package com.l7tech.server.security.cert;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.whirlycott.cache.Cache;

import com.l7tech.server.util.HttpClientFactory;
import com.l7tech.server.ServerConfig;
import com.l7tech.common.util.TimeUnit;
import com.l7tech.common.util.WhirlycacheFactory;

/**
 * Caching wrapper for OCSPClient functionality.
 *
 * @author Steve Jones
 */
public class OCSPCache {

    //- PUBLIC

    /**
     * Create an OCSPCache that uses the given http client factory and config.
     *
     * @param httpClientFactory The HTTP client factory to use (must not be null)
     * @param serverConfig The server configuration to use (must not be null)
     */
    public OCSPCache(final HttpClientFactory httpClientFactory,
                     final ServerConfig serverConfig) {
        if (httpClientFactory==null) throw new IllegalArgumentException("httpClientFactory must not be null");
        if (serverConfig==null) throw new IllegalArgumentException("serverConfig must not be null");

        this.httpClientFactory = httpClientFactory;
        this.serverConfig = serverConfig;
        this.certValidationCache =
                WhirlycacheFactory.createCache("OCSPResponseCache", 1000, 57, WhirlycacheFactory.POLICY_LRU);
    }

    /**
     * Get the OCSPStatus for the given certificate from the specified responder.
     *
     * <p>Note that the authorizer will not be consulted if the response is from
     * cache.</p>
     *
     * @param responderUrl The URL of the responder to query.
     * @param certificate The certificate whose status is to be queried
     * @param issuerCertificate The issuer of the certificate being checked
     * @param responseAuthorizer The authorizer to use
     * @return The status
     * @throws OCSPClient.OCSPClientException If the status cannot be obtained
     */
    public OCSPClient.OCSPStatus getOCSPStatus(final String responderUrl,
                                               final X509Certificate certificate,
                                               final X509Certificate issuerCertificate,
                                               final OCSPClient.OCSPCertificateAuthorizer responseAuthorizer)
        throws OCSPClient.OCSPClientException
    {

        OcspKey key = buildOcspKey(responderUrl, certificate);

        OCSPClient.OCSPStatus status = (OCSPClient.OCSPStatus) certValidationCache.retrieve(key);
        if (status != null) {
            logger.log(Level.INFO, "Using cached OCSP response.");
        } else {
            OCSPClient ocsp = new OCSPClient(httpClientFactory.createHttpClient(), responderUrl, issuerCertificate, responseAuthorizer);
            status = ocsp.getRevocationStatus(certificate, useNonce(), true);
            certValidationCache.store(key, status, getExpiryTime(status.getExpiry()));
        }

        return status;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(OCSPCache.class.getName());

    private static final int ONE_MINUTE = TimeUnit.MINUTES.getMultiplier();

    private static final String PROP_EXPIRY_DEFAULT = "pkixOCSP.expiry";
    private static final String PROP_EXPIRY_MIN = "pkixOCSP.minExpiryAge";
    private static final String PROP_EXPIRY_MAX = "pkixOCSP.maxExpiryAge";
    private static final String PROP_USE_NONCE = "pkixOCSP.useNonce";

    private final Cache certValidationCache;
    private final HttpClientFactory httpClientFactory;
    private final ServerConfig serverConfig;

    /**
     * Calculate expiry for response. 
     */
    private long getExpiryTime(final long responseExpiry) {
        long expiry;

        long timeNow = System.currentTimeMillis();
        if ( responseExpiry <= 0 ) {
            expiry = timeNow + getDefaultExpiry();
        } else {
            long updateTime = responseExpiry;
            long updatePeriod = updateTime - timeNow;

            if ( updatePeriod > getMaxExpiry() ) {
                expiry = timeNow + getMaxExpiry();
            } else if ( updatePeriod < getMinExpiry() ) {
                expiry = timeNow + getMinExpiry();
            } else {
                expiry = updateTime;
            }
        }

        return expiry;
    }

    /**
     * Get the default expiry time in millis from configuration
     */
    private long getDefaultExpiry() {
        return getExpiry(PROP_EXPIRY_DEFAULT);
    }

    /**
     * Get the minimum expiry time in millis from configuration
     */
    private long getMinExpiry() {
        return getExpiry(PROP_EXPIRY_MIN);
    }

    /**
     * Get the maximum expiry time in millis from configuration
     */
    private long getMaxExpiry() {
        return getExpiry(PROP_EXPIRY_MAX);
    }

    /**
     * Get the use nonce flag from configuration
     */
    private boolean useNonce() {
        boolean useNonce = true;
        String value = serverConfig.getPropertyCached(PROP_USE_NONCE, 30000);
        if (value != null) {
            useNonce = Boolean.valueOf(value);    
        }
        return useNonce;
    }

    /**
     * Get the named timeunit from configuration
     */
    private long getExpiry(String name) {
        return serverConfig.getTimeUnitPropertyCached(name, ONE_MINUTE, 30000);
    }

    /**
     * Build and OCSP response caching key
     */
    private OcspKey buildOcspKey(String responderUrl, X509Certificate certificate) throws OCSPClient.OCSPClientException {
        try {
            return new OcspKey(responderUrl, certificate);
        } catch (CertificateException ce) {
            throw new OCSPClient.OCSPClientException("Error processing certificate", ce);
        }
    }

    /**
     * Key based on an X.509 Certificate and OCSP Responder URL
     */
    private static final class OcspKey {
        private final String url;
        private final byte[] encodedCertificate;
        private final int hashCode;

        private OcspKey(String url, X509Certificate certificate) throws CertificateEncodingException {
            this.url = url;
            this.encodedCertificate = certificate.getEncoded();
            this.hashCode = Arrays.hashCode(encodedCertificate) + (13 * url.hashCode());
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OcspKey ocspKey = (OcspKey) o;

            if (!Arrays.equals(encodedCertificate, ocspKey.encodedCertificate)) return false;
            if (!url.equals(ocspKey.url)) return false;

            return true;
        }

        public int hashCode() {
            return hashCode;
        }
    }
}
