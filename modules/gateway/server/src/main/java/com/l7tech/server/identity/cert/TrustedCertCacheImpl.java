package com.l7tech.server.identity.cert;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;

import java.util.Collection;
import java.util.Collections;

/**
 * Used with a caching interceptor.
 */
public class TrustedCertCacheImpl implements TrustedCertCache {

    //- PUBLIC

    public TrustedCertCacheImpl( final TrustedCertManager trustedCertManager ) {
        this.trustedCertManager = trustedCertManager;
    }

    @Override
    public TrustedCert findByPrimaryKey( final Goid oid ) throws FindException {
        return immutable( trustedCertManager.findByPrimaryKey( oid ) );
    }

    @Override
    public Collection<TrustedCert> findByName( final String name ) throws FindException {
        return immutable( trustedCertManager.findByName( name ) );
    }

    @Override
    public Collection<TrustedCert> findByTrustFlag(TrustedCert.TrustedFor trustFlag) throws FindException {
        return immutable( trustedCertManager.findByTrustFlag(trustFlag) );
    }

    @Override
    public Collection<TrustedCert> findBySubjectDn( final String dn ) throws FindException {
        return immutable( trustedCertManager.findBySubjectDn( dn ) );
    }

    //- PRIVATE

    private final TrustedCertManager trustedCertManager;

    private TrustedCert immutable( final TrustedCert trustedCert ) {
        if ( trustedCert != null ) {
            trustedCert.setReadOnly();
        }
        return trustedCert;
    }

    private Collection<TrustedCert> immutable( final Collection<TrustedCert> trustedCerts ) {
        for ( TrustedCert trustedCert : trustedCerts ) {
            trustedCert.setReadOnly();            
        }
        return Collections.unmodifiableCollection( trustedCerts );
    }
}
