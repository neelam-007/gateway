/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.cert;

import com.l7tech.common.security.CertificateExpiry;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.objectmodel.*;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Provides access to CRUD functionality for {@link TrustedCert} objects.
 * @author alex
 * @version $Revision$
 */
public interface TrustedCertManager extends EntityManager {
    /**
     * Retrieves the {@link TrustedCert} with the specified oid, or null if it does not exist.
     * @param oid the oid of the {@link TrustedCert} to retrieve
     * @return the retrieved {@link TrustedCert}, or null if it does not exist.
     * @throws FindException if the retrieval fails for any reason other than nonexistence
     */
    TrustedCert findByPrimaryKey(long oid) throws FindException;

    /**
     * Retrieves the {@link TrustedCert} with the specified DN, or null if it does not exist.
     * <b>NOTE:</b> The corresponding field in the database must have a unique constraint!
     * @param dn the DN of the {@link TrustedCert} to retrieve
     * @return the retrieved {@link TrustedCert}, or null if it does not exist.
     * @throws FindException if the retrieval fails for any reason other than nonexistence
     */
    TrustedCert findBySubjectDn(String dn) throws FindException;

    /**
     * Saves a new {@link TrustedCert}, returning the oid that was generated.
     * @param cert The new {@link TrustedCert} to be saved.
     * @return the oid of the newly saved object
     * @throws SaveException if the {@link TrustedCert} cannot be saved for any reason.
     */
    long save(TrustedCert cert) throws SaveException;

    /**
     * Updates an existing {@link TrustedCert} in the database.
     * @param cert an existing {@link TrustedCert} to be updated
     * @throws UpdateException if the {@link TrustedCert} cannot be updated for any reason.
     */
    void update(TrustedCert cert) throws UpdateException;

    /**
     * Deletes an existing {@link TrustedCert} from the database.
     * @param oid the oid of the {@link TrustedCert} to be deleted
     * @throws FindException if the {@link TrustedCert} does not exist or cannot be retrieved prior to deletion
     * @throws DeleteException if the {@link TrustedCert} cannot be deleted for any reason other than nonexistence
     */
    void delete(long oid) throws FindException, DeleteException;

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
    TrustedCert getCachedCertBySubjectDn(String dn, int maxAge) throws FindException, IOException, CertificateException;

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
    TrustedCert getCachedCertByOid(long oid, int maxAge) throws FindException, IOException, CertificateException;

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
}
