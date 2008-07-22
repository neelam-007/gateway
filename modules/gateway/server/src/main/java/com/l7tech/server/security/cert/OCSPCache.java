package com.l7tech.server.security.cert;

import com.l7tech.server.audit.Auditor;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.util.TimeUnit;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.HttpClientFactory;
import com.l7tech.security.types.CertificateValidationResult;
import com.whirlycott.cache.Cache;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;

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
     * @param auditor The auditor to use
     * @return The status
     * @throws OCSPClient.OCSPClientException If the status cannot be obtained
     */
    public OCSPClient.OCSPStatus getOCSPStatus(final String responderUrl,
                                               final X509Certificate certificate,
                                               final X509Certificate issuerCertificate,
                                               final OCSPClient.OCSPCertificateAuthorizer responseAuthorizer,
                                               final Auditor auditor,
                                               final CertificateValidationResult onNetworkFailure)
        throws OCSPClient.OCSPClientException
    {

        OcspKey key = buildOcspKey(responderUrl, certificate);

        Set<OcspKey> recursionDetectionSet = RECURSION_SET.get();
        if ( recursionDetectionSet.contains(key) ) {
            throw new OCSPClientRecursionException("Recursive or circular OCSP request for '"+certificate.getSubjectDN()+"'.");
        } else {
            recursionDetectionSet.add(key);
        }

        try {
            OCSPClient.OCSPStatus status;
            OCSPClient.OCSPStatus cachedStatus = null;
            OcspValue ocspValue = (OcspValue) certValidationCache.retrieve(key);

            //grab the cached status, regardless if it's expired already
            if ( ocspValue != null ) {
                cachedStatus = ocspValue.status;
            }

            if (ocspValue != null && !ocspValue.isExpired()) {
                status = ocspValue.status;
                auditor.logAndAudit(SystemMessages.CERTVAL_REV_CACHE_HIT, "OCSP", certificate.getSubjectDN().toString());
            } else {
                auditor.logAndAudit(SystemMessages.CERTVAL_REV_CACHE_MISS, "OCSP", certificate.getSubjectDN().toString());
                OCSPClient ocsp = new OCSPClient(httpClientFactory.createHttpClient(), responderUrl, issuerCertificate, responseAuthorizer);

                try {
                    //attempt to get OCSP status for the certificate
                    status = ocsp.getRevocationStatus(certificate, useNonce(), true);
                }
                catch (OCSPClient.OCSPClientException oce) {
                    //something has gone wrong trying to grab the revocation status from the server, we'll need to reuse
                    //the cache version
                    if ( cachedStatus != null) {
                        status = cachedStatus;

                        auditor.logAndAudit(SystemMessages.CERTVAL_REV_USE_CACHE, "OCSP", certificate.getSubjectDN().toString(), new Date(status.getExpiry()).toString());
                        certValidationCache.store(key, new OcspValue(cachedStatus, status.getExpiry()), status.getExpiry());

                        return status;  //return the cached version
                    }
                    else {
                        //we don't even have status value, so we'll need to throw the error
                        throw oce;
                    }
                }

                long timeNow = System.currentTimeMillis();
                long expiryPeriod = getExpiryPeriod(status.getExpiry(), timeNow);
                certValidationCache.store(key, new OcspValue(status, timeNow + expiryPeriod), expiryPeriod);
            }

            return status;
        } finally {
            recursionDetectionSet.remove(key);     
        }
    }

    /**
     * Exception class for recursion
     */
    public static final class OCSPClientRecursionException extends OCSPClient.OCSPClientException {
        public OCSPClientRecursionException(final String message) {
            super(message);
        }
        public OCSPClientRecursionException(final String message, Throwable cause) {
            super(message, cause);
        }
    }

    //- PRIVATE

    private static final int ONE_MINUTE = TimeUnit.MINUTES.getMultiplier();
    private static final ThreadLocal<Set<OcspKey>> RECURSION_SET = new ThreadLocal() {
        protected Set<OcspKey> initialValue() {
            return new HashSet<OcspKey>();
        }
    };

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
    private long getExpiryPeriod(final long responseExpiry, final long timeNow) {
        long expiry;

        if ( responseExpiry <= 0 ) {
            expiry = getDefaultExpiry();
        } else {
            long updateTime = responseExpiry;
            long updatePeriod = updateTime - timeNow;

            if ( updatePeriod > getMaxExpiry() ) {
                expiry = getMaxExpiry();
            } else if ( updatePeriod < getMinExpiry() ) {
                expiry = getMinExpiry();
            } else {
                expiry = updatePeriod;
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

    /**
     * Value is just a status with a timestamp (of the actual expiry)
     */
    private static final class OcspValue {
        private final OCSPClient.OCSPStatus status;
        private final long expiry;

        private OcspValue(OCSPClient.OCSPStatus status, long expiry) {
            this.status = status;
            this.expiry = expiry;
        }

        private boolean isExpired() {
            boolean expired = true;

            if (expiry > System.currentTimeMillis()) {
                expired = false;        
            }

            return expired;
        }
    }
}
