/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.config.client.beans.ConfigurationBean;

import java.security.cert.X509Certificate;

/** @author alex */
public class ConfiguredTrustedCert extends ConfigurationBean<X509Certificate> {

    ConfiguredTrustedCert(X509Certificate cert) {
        super("host.controller.remoteNodeManagement.trustedCert", "Trusted Certificate", cert, null, true);
    }

    @Override
    public String getShortValueDescription() {
        X509Certificate cert = getConfigValue();
        if (cert == null) return null;
        return cert.getSubjectDN().getName();
    }
}
