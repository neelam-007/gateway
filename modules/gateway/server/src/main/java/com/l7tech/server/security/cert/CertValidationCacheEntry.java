/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.cert;

import com.l7tech.security.cert.TrustedCert;
import com.l7tech.common.io.CertUtils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.math.BigInteger;

/**
 * @author alex
*/
class CertValidationCacheEntry {
    final TrustedCert tce;
    final X509Certificate cert;
    final String subjectDn;
    final String ski;
    final String issuerDn;
    final BigInteger serial;

    CertValidationCacheEntry(TrustedCert tce) {
        this.tce = tce;
        this.cert = tce.getCertificate();
        this.subjectDn = tce.getSubjectDn();
        this.ski = tce.getSki();
        this.issuerDn = CertUtils.getIssuerDN( cert );
        this.serial = this.cert.getSerialNumber();
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CertValidationCacheEntry that = (CertValidationCacheEntry) o;

        if (tce != null ? !tce.equals(that.tce) : that.tce != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (tce != null ? tce.hashCode() : 0);
    }
}
