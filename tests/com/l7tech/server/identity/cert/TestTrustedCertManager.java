/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.identity.cert;

import com.l7tech.common.security.CertificateExpiry;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.*;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * @author emil
 * @version Dec 15, 2004
 */
public class TestTrustedCertManager extends EntityManagerStub<TrustedCert> implements TrustedCertManager {
    public TrustedCert findBySubjectDn(String dn) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public void update(TrustedCert cert) throws UpdateException {
        throw new RuntimeException("Not implemented");
    }

    public void delete(long oid) throws FindException, DeleteException {
        throw new RuntimeException("Not implemented");
    }

    public TrustedCert getCachedCertBySubjectDn(String dn, int maxAge) throws FindException, CertificateException {
        throw new RuntimeException("Not implemented");
    }

    public TrustedCert getCachedCertByOid(long oid, int maxAge) throws FindException, CertificateException {
        throw new RuntimeException("Not implemented");
    }

    public void logWillExpire(TrustedCert cert, CertificateExpiry e) {
        throw new RuntimeException("Not implemented");
    }

    public void checkSslTrust(X509Certificate[] serverCertChain) throws CertificateException {
        throw new RuntimeException("Not implemented");
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