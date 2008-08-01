/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.cert;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Cachable;

import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.List;

/**
 * Provides access to CRUD functionality for {@link TrustedCert} objects.
 * @author alex
 * @version $Revision$
 */
public interface TrustedCertManager extends EntityManager<TrustedCert, EntityHeader> {
    /**
     * Retrieves every {@link TrustedCert} with the specified DN.
     *
     * @param dn the DN of the {@link TrustedCert}s to retrieve
     * @return a Collection of matching TrustedCert instances.  May be empty but never null.
     * @throws FindException if the retrieval fails for any reason other than nonexistence
     */
    Collection<TrustedCert> findBySubjectDn(String dn) throws FindException;

    /**
     * Retrieves the TrustedCert instances with the specified subject DN from a cache,
     * if it was cached less than maxAge milliseconds ago.
     * <p>
     * If the cached version is more than the specified maximum age, the manager will check
     * the database for the latest version.  If it has been updated, it will retrieve the new
     * version and update the cache.
     *
     * @param dn the Subject DN to search by
     * @return the TrustedCert with the specified Subject DN, or null if no such cert exists.
     * @throws FindException if the TrustedCert cannot be found.
     */
    @Cachable(relevantArg=0,maxAge=5000)
    Collection<TrustedCert> getCachedCertsBySubjectDn(String dn) throws FindException;

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
