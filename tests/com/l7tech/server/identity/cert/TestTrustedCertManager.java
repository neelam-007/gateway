/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.identity.cert;

import com.l7tech.objectmodel.*;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.CertificateExpiry;
import com.l7tech.identity.cert.TrustedCertManager;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author emil
 * @version Dec 15, 2004
 */
public class TestTrustedCertManager implements TrustedCertManager {

    public TrustedCert findByPrimaryKey(long oid) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public TrustedCert findBySubjectDn(String dn) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public long save(TrustedCert cert) throws SaveException {
        throw new RuntimeException("Not implemented");
    }

    public void update(TrustedCert cert) throws UpdateException {
        throw new RuntimeException("Not implemented");
    }

    public void delete(long oid) throws FindException, DeleteException {
        throw new RuntimeException("Not implemented");
    }

    public TrustedCert getCachedCertBySubjectDn(String dn, int maxAge) throws FindException, IOException, CertificateException {
        throw new RuntimeException("Not implemented");
    }

    public TrustedCert getCachedCertByOid(long oid, int maxAge) throws FindException, IOException, CertificateException {
        throw new RuntimeException("Not implemented");
    }

    public void logWillExpire(TrustedCert cert, CertificateExpiry e) {
        throw new RuntimeException("Not implemented");
    }

    public void checkSslTrust(X509Certificate[] serverCertChain) throws CertificateException {
    }

    public Collection findAllHeaders() throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public Collection findAll() throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public Integer getVersion(long oid) throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public Map findVersionMap() throws FindException {
        throw new RuntimeException("Not implemented");
    }

    public Entity getCachedEntity(long o, int maxAge) throws FindException, CacheVeto {
        throw new RuntimeException("Not implemented");
    }
}