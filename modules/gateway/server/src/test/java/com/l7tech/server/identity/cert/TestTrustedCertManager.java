/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.identity.cert;

import com.l7tech.common.io.CertificateExpiry;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityManagerStub;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Collection;

/**
 * @author emil
 * @version Dec 15, 2004
 */
public class TestTrustedCertManager extends EntityManagerStub<TrustedCert,EntityHeader> implements TrustedCertManager {
    public Collection<TrustedCert> findBySubjectDn(String dn) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public Collection<TrustedCert> getCachedCertsBySubjectDn(String dn) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public TrustedCert getCachedCertByOid(long oid, int maxAge) throws FindException, CertificateException {
        throw new RuntimeException("Not implemented");
    }

    public void logWillExpire(TrustedCert cert, CertificateExpiry e) {
        throw new RuntimeException("Not implemented");
    }

    public void checkSslTrust(X509Certificate[] serverCertChain) throws CertificateException {
    }

    public List findByThumbprint(String thumbprint) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public List findBySki(String ski) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public Class getImpClass() {
        return TrustedCert.class;
    }

    public Class getInterfaceClass() {
        return TrustedCert.class;
    }

    public EntityType getEntityType() {
        return EntityType.TRUSTED_CERT;
    }

    public String getTableName() {
        return "trusted_certs";
    }
}
