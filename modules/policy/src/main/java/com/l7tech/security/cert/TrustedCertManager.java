/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.cert;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.List;

/**
 * Provides access to CRUD functionality for {@link TrustedCert} objects.
 */
public interface TrustedCertManager extends GoidEntityManager<TrustedCert, EntityHeader> {
    /**
     * Retrieves every {@link TrustedCert} with the specified DN.
     *
     * @param dn the DN of the {@link TrustedCert}s to retrieve
     * @return a Collection of matching TrustedCert instances.  May be empty but never null.
     * @throws FindException if the retrieval fails for any reason other than nonexistence
     */
    Collection<TrustedCert> findBySubjectDn(String dn) throws FindException;

    /**
     * @return {@link TrustedCert}s with the matching base64'd SHA-1 thumbprint. Never null, but may be empty.
     * @param thumbprint the base64'd SHA-1 thumbprint value to search for. May be null.
     */
    List<TrustedCert> findByThumbprint(String thumbprint) throws FindException;

    /**
     * @return {@link TrustedCert}s with the matching base64'd SKI. Never null, but may be empty.
     * @param ski the base64'd SKI value to search for. May be null.
     */
    List<TrustedCert> findBySki(String ski) throws FindException;

    /**
     * Finds TrustedCerts whose issuer DN matches the specified X500Principal, and having the specified serial number.
     *
     * @param issuer the X.500 Principal of the issuer DN. Must not be null.
     * @param serial the serial number of the subject certificate. Must not be null.
     * @return {@link TrustedCert}s with the matching Issuer DN and serial number.  Never null, but may be empty.
     */
    List<TrustedCert> findByIssuerAndSerial(X500Principal issuer, BigInteger serial) throws FindException;

    /**
     * Retrieves every {@link TrustedCert} with the given name.
     *
     * @param name the name of the {@link TrustedCert}s to retrieve
     * @return a Collection of matching TrustedCert instances.  May be empty but never null.
     * @throws FindException if the retrieval fails for any reason other than nonexistence
     */
    Collection<TrustedCert> findByName(String name) throws FindException;

    /**
     * Find all TrustedCert entities that are trusted for the specified behavior.
     * <p/>
     * Note that this method currently performs a full table scan.
     *
     * @param trustFlag the trust flag to query.  Required.
     * @return all matching TrustedCert entities.  May be empty but never null.
     * @throws FindException if the retrieval fails for any reason other than nonexistence
     */
    Collection<TrustedCert> findByTrustFlag(TrustedCert.TrustedFor trustFlag) throws FindException;

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
