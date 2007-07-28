/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.cert;

import com.l7tech.common.audit.Auditor;

import java.io.IOException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.cert.CRLException;

/**
 * @author alex
 */
public interface CrlCache {
    String[] getCrlUrlsFromCertificate(X509Certificate cert, Auditor auditor) throws IOException;
    X509CRL getCrl(String url, Auditor auditor) throws CRLException, IOException;
}
