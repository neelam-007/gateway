package com.l7tech.server.identity.cert;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GoidEntityManagerStub;
import com.l7tech.util.Functions;
import com.l7tech.util.Functions.Unary;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TestTrustedCertManager extends GoidEntityManagerStub<TrustedCert,EntityHeader> implements TrustedCertManager, TrustedCertCache {

    private final DefaultKey defaultKey;

    public TestTrustedCertManager( final DefaultKey defaultKey ) {
        this.defaultKey = defaultKey;    
    }

    public TestTrustedCertManager( final TrustedCert... entitiesIn ) {
        super( entitiesIn );
        this.defaultKey = null;
    }

    @Override
    public Collection<TrustedCert> findBySubjectDn( final String dn ) throws FindException {
        return Functions.grep( new ArrayList<TrustedCert>(), entities.values(), new Unary<Boolean, TrustedCert>() {
            @Override
            public Boolean call( final TrustedCert trustedCert ) {
                return dn.equals( trustedCert.getSubjectDn() ) ;
            }
        } );
    }

    @Override
    public List<TrustedCert> findByIssuerAndSerial( final X500Principal issuer,
                                                    final BigInteger serial) throws FindException {
        return Functions.grep( new ArrayList<TrustedCert>(), entities.values(), new Unary<Boolean, TrustedCert>() {
            @Override
            public Boolean call( final TrustedCert trustedCert ) {
                final X509Certificate cert = trustedCert.getCertificate();
                return cert.getIssuerDN().equals(issuer) && cert.getSerialNumber().equals(serial);
            }
        } );
    }

    @Override
    public List<TrustedCert> findByThumbprint( final String thumbprint ) throws FindException {
        return Functions.grep( new ArrayList<TrustedCert>(), entities.values(), new Unary<Boolean, TrustedCert>() {
            @Override
            public Boolean call( final TrustedCert trustedCert ) {
                return thumbprint.equals( trustedCert.getThumbprintSha1() );
            }
        } );
    }

    @Override
    public List<TrustedCert> findBySki( final String ski ) throws FindException {
        return Functions.grep( new ArrayList<TrustedCert>(), entities.values(), new Unary<Boolean, TrustedCert>() {
            @Override
            public Boolean call( final TrustedCert trustedCert ) {
                return ski.equals( trustedCert.getSki() );
            }
        } );
    }

    @Override
    public Collection<TrustedCert> findByName( final String name ) throws FindException {
        if ( "defaultkey".equalsIgnoreCase(name) ) {
            try {
                TrustedCert tc = new TrustedCert();
                tc.setCertificate( defaultKey.getSslInfo().getCertificate() );
                return Collections.singleton( tc );
            } catch (IOException e) {
                throw new FindException("Error finding default key",e);
            }
        } else {
            return Functions.grep( new ArrayList<TrustedCert>(), entities.values(), new Unary<Boolean, TrustedCert>() {
                @Override
                public Boolean call( final TrustedCert trustedCert ) {
                    return name.equalsIgnoreCase( trustedCert.getName() );
                }
            } );
        }
    }

    @Override
    public Collection<TrustedCert> findByTrustFlag( final TrustedCert.TrustedFor trustFlag ) throws FindException {
        return Functions.grep( new ArrayList<TrustedCert>(), entities.values(), new Unary<Boolean, TrustedCert>() {
            @Override
            public Boolean call( final TrustedCert trustedCert ) {
                return trustedCert.isTrustedFor( trustFlag );
            }
        } );
    }

    @Override
    public TrustedCert findByOldOid(long oid) throws FindException {
        return null;
    }

    @Override
    public Class<TrustedCert> getImpClass() {
        return TrustedCert.class;
    }

    @Override
    public Class<TrustedCert> getInterfaceClass() {
        return TrustedCert.class;
    }
}
