/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.config.client.beans.ConfigurationBean;

import java.security.cert.X509Certificate;

/** @author alex */
public class ConfiguredTrustedCert extends ConfigurationBean<X509Certificate> {
    private final NewTrustedCertFactory factory;

    /**
     * @param cert the certificate that's trusted
     * @param factory the factory that was originally used to create this trusted cert bean, or null to skip the factory maintenance
     */
    ConfiguredTrustedCert(X509Certificate cert, NewTrustedCertFactory factory) {
        super("host.controller.remoteNodeManagement.trustedCert", "Trusted Certificate", cert, null, true);
        this.factory = factory;
    }

    @Override
    public String getShortValueDescription() {
        X509Certificate cert = getConfigValue();
        if (cert == null) return null;
        return cert.getSubjectDN().getName();
    }

    @Override
    public void onDelete() {
        if (factory != null) factory.release();
    }

}
