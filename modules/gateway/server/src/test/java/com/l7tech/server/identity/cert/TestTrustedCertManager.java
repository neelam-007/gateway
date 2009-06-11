/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.cert;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.EntityManagerStub;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestTrustedCertManager extends EntityManagerStub<TrustedCert,EntityHeader> implements TrustedCertManager, TrustedCertCache {
    @Override
    public Collection<TrustedCert> findBySubjectDn(String dn) throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TrustedCert> findByIssuerAndSerial(X500Principal issuer, BigInteger serial) throws FindException {
        List<TrustedCert> tcs = new ArrayList<TrustedCert>();
        for (TrustedCert trustedCert : entities.values()) {
            X509Certificate cert = trustedCert.getCertificate();
            if (cert.getIssuerDN().equals(issuer) && cert.getSerialNumber().equals(serial)) tcs.add(trustedCert);
        }
        return tcs;
    }

    @Override
    public List<TrustedCert> findByThumbprint(String thumbprint) throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TrustedCert> findBySki(String ski) throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<TrustedCert> findByName(String name) throws FindException {
        throw new UnsupportedOperationException();
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
