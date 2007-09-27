/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.cert;

import com.l7tech.common.security.CertificateExpiry;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.util.Cachable;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Provides access to CRUD functionality for {@link TrustedCert} objects.
 * @author alex
 * @version $Revision$
 */
public interface TrustedCertManager extends EntityManager<TrustedCert, EntityHeader> {
    /**
     * Retrieves the {@link TrustedCert} with the specified DN, or null if it does not exist.
     * <b>NOTE:</b> The corresponding field in the database must have a unique constraint!
     * @param dn the DN of the {@link TrustedCert} to retrieve
     * @return the retrieved {@link TrustedCert}, or null if it does not exist.
     * @throws FindException if the retrieval fails for any reason other than nonexistence
     */
    TrustedCert findBySubjectDn(String dn) throws FindException;

    /**
     * Retrieves the TrustedCert with the specified subject DN from a cache,
     * if it was cached less than maxAge milliseconds ago.
     * <p>
     * If the cached version is more than the specified maximum age, the manager will check
     * the database for the latest version.  If it has been updated, it will retrieve the new
     * version and update the cache.
     *
     * @param dn the Subject DN to search by
     * @param maxAge the maximum age of cache entries that will be returned without a database version check
     * @return the TrustedCert with the specified Subject DN, or null if no such cert exists.
     * @throws FindException if the TrustedCert cannot be found.
     */
    @Cachable(relevantArg=0,maxAge=1000)
    TrustedCert getCachedCertBySubjectDn(String dn, int maxAge) throws FindException, CertificateException;

    /**
     * Retrieves the TrustedCert with the specified oid from a cache,
     * if it was cached less than maxAge milliseconds ago.
     * <p>
     * If the cached version is more than the specified maximum age, the manager will check
     * the database for the latest version.  If it has been updated, it will retrieve the new
     * version and update the cache.
     *
     * @param oid the oid to search by
     * @param maxAge the maximum age of cache entries that will be returned without a database version check
     * @return the TrustedCert with the specified Subject DN, or null if no such cert exists.
     * @throws FindException if the TrustedCert cannot be found.
     */
    TrustedCert getCachedCertByOid(long oid, int maxAge) throws FindException, CertificateException;

    /**
     * Logs a good warning message for a cert that will expire soon
     */
    void logWillExpire( TrustedCert cert, CertificateExpiry e );

    /**
     * Checks whether the certificate at the top of the specified chain is trusted for outbound SSL connections.
     * <p>
     * This will be true if either the specific certificate has the {@link com.l7tech.common.security.TrustedCert#isTrustedForSsl()}
     * option set, or the signing cert that comes next in the chain has the {@link com.l7tech.common.security.TrustedCert#isTrustedForSigningServerCerts()}
     * option set.
     * <p>
     * @param serverCertChain the certificate chain
     * @throws CertificateException
     */
    void checkSslTrust(X509Certificate[] serverCertChain) throws CertificateException;

    /**
     * @return {@link TrustedCert}s with the matching base64'd SHA-1 thumbprint. Never null, but may be empty.
     * @param thumbprint the base64'd SHA-1 thumbprint value to search for. May be null.
     */
    List findByThumbprint(String thumbprint) throws FindException;

    /**
     * @return {@link TrustedCert}s with the matching base64'd SKI. Never null, but may be empty.
     * @param ski the base64'd SKI value to search for. May be null.
     */
    List findBySki(String ski) throws FindException;

    /**
     * Subclass of certificate exception thrown when a certificate is not known.
     */
    public static final class UnknownCertificateException extends CertificateException {

        public UnknownCertificateException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnknownCertificateException(String msg) {
            super(msg);
        }
    }
}
