package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCert;


/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertInfo {
    TrustedCert trustedCert = new TrustedCert();
    Object certDataSource;

    public CertInfo() {
        this.certDataSource = null;
    }

    public TrustedCert getTrustedCert() {
        return trustedCert;
    }

    public void setTrustedCert(TrustedCert trustedCert) {
        this.trustedCert = trustedCert;
    }

    public Object getCertDataSource() {
        return certDataSource;
    }

    public void setCertDataSource(Object certDataSource) {
        this.certDataSource = certDataSource;
    }



}
