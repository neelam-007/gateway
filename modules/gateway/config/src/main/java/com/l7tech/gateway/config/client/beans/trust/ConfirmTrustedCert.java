/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.gateway.config.client.beans.BooleanConfigurableBean;
import com.l7tech.gateway.config.client.beans.ConfigResult;
import com.l7tech.gateway.config.client.beans.ConfigurationContext;

import java.security.cert.X509Certificate;

/** @author alex */
public class ConfirmTrustedCert extends BooleanConfigurableBean {
    private final X509Certificate cert;

    ConfirmTrustedCert(X509Certificate cert) {
        super("host.controller.remoteNodeManagement.confirmTrustedCert", "Confirm Trusted Certificate", false);
        this.cert = cert;
    }

    @Override
    public ConfigResult onConfiguration(Boolean value, ConfigurationContext context) {
        if (value) {
            return ConfigResult.pop(new ConfiguredTrustedCert(cert, true));
        } else {
            return ConfigResult.pop();
        }
    }
}
