/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.cert;

import com.l7tech.common.security.TrustedCert;

import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author alex
*/
class CertValidationCacheEntry {
    final TrustedCert tce;
    final X509Certificate cert;
    final String subjectDn;

    private CertPath path;

    CertValidationCacheEntry(TrustedCert tce) throws CertificateException {
        this.tce = tce;
        this.cert = tce.getCertificate();
        this.subjectDn = tce.getSubjectDn();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CertValidationCacheEntry that = (CertValidationCacheEntry) o;

        if (tce != null ? !tce.equals(that.tce) : that.tce != null) return false;

        return true;
    }

    public int hashCode() {
        return (tce != null ? tce.hashCode() : 0);
    }
}
