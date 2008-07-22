/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.security.cert.CrlCache;
import com.l7tech.server.util.ServerCertUtils;

import java.io.IOException;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;

/**
 * @author alex
 */
public class TestCrlCache implements CrlCache {
    public String[] getCrlUrlsFromCertificate(X509Certificate cert, Audit auditor) throws IOException {
        return ServerCertUtils.getCrlUrls(cert);
    }

    public X509CRL getCrl(String url, Audit auditor) throws CRLException, IOException {
        return null;
    }
}
