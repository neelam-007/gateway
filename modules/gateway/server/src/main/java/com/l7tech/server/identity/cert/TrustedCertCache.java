package com.l7tech.server.identity.cert;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.Cacheable;

import java.util.Collection;

/**
 * Provides cached TrustedCert access.
 */
public interface TrustedCertCache {

    /**
     * Retrieves immutable TrustedCert instance with the specified id.
     *
     * @param oid the identifier for the trusted cert
     * @return the TrustedCert with the specified identififer, or null if no such cert exists.
     * @throws com.l7tech.objectmodel.FindException if an error occurs.
     */
    @Cacheable(relevantArg=0,maxAge=5000)
    TrustedCert findByPrimaryKey( Goid oid ) throws FindException;

    /**
     * Retrieves immutable TrustedCert instances with the specified subject DN.
     *
     * @param dn the Subject DN to search by
     * @return a Collection of matching TrustedCert instances.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if an error occurs.
     */
    @Cacheable(relevantArg=0,maxAge=5000)
    Collection<TrustedCert> findBySubjectDn(String dn) throws FindException;

    /**
     * Retrieves immutable {@link TrustedCert}s with the given name.
     *
     * @param name the name of the {@link TrustedCert}s to retrieve
     * @return a Collection of matching TrustedCert instances.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if an error occurs.
     */
    @Cacheable(relevantArg=0,maxAge=5000)
    Collection<TrustedCert> findByName(String name) throws FindException;

    /**
     * Retrieves immutable {@link TrustedCert}s with the given trust flags.
     *
     * @param trustFlag trust flag to look up.  Required.
     * @return a Collection of matching TrustedCert instances.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if an error occurs.
     */
    @Cacheable(relevantArg=0,maxAge=120000)
    Collection<TrustedCert> findByTrustFlag(TrustedCert.TrustedFor trustFlag) throws FindException;
}
