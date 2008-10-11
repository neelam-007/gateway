/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.host;

import java.security.cert.X509Certificate;

/** @author alex */
public class TrustedCertFeature extends HostFeature {
    private final X509Certificate cert;

    public TrustedCertFeature(PCHostConfig parent, X509Certificate cert) {
        super(parent, HostFeatureType.TRUSTED_CERT);
        this.cert = cert;
    }

    public X509Certificate getCert() {
        return cert;
    }
}
