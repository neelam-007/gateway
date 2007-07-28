/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.util.CertUtils;
import com.l7tech.server.security.cert.CrlCache;

import java.io.IOException;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;

/**
 * @author alex
 */
public class TestCrlCache implements CrlCache {
    public String[] getCrlUrlsFromCertificate(X509Certificate cert, Auditor auditor) throws IOException {
        return CertUtils.getCrlUrls(cert);
    }

    public X509CRL getCrl(String url, Auditor auditor) throws CRLException, IOException {
        return null;
    }
}
