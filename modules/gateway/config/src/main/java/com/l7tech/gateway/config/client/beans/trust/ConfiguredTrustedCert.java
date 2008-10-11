/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.gateway.config.client.beans.ConfigurationBean;
import com.l7tech.gateway.config.client.beans.Repeatable;

import java.security.cert.X509Certificate;

/** @author alex */
public class ConfiguredTrustedCert extends ConfigurationBean<X509Certificate> implements Repeatable {
    private int index;
    private final boolean existing;

    ConfiguredTrustedCert(X509Certificate cert, boolean existing) {
        super("host.controller.remoteNodeManagement.trustedCert", "Trusted Certificate", cert, true);
        this.existing = existing;
    }

    @Override
    public String getShortValueDescription() {
        X509Certificate cert = getConfigValue();
        if (cert == null) return null;
        return cert.getSubjectDN().getName();
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public boolean isExisting() {
        return existing;
    }


}
