/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.cert;

import com.l7tech.gateway.common.audit.Audit;

import java.io.IOException;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;

/**
 * @author alex
 */
public interface CrlCache {
    String[] getCrlUrlsFromCertificate(X509Certificate cert, Audit auditor) throws IOException;
    X509CRL getCrl(String url, Audit auditor) throws CRLException, IOException;
}
