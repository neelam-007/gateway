/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.gateway.config.client.beans.ConfigurableBeanFactory;

/** @author alex */
public class NewTrustedCertFactory extends ConfigurableBeanFactory<TrustedCertUrl> {
    protected NewTrustedCertFactory() {
        super("host.controller.remoteNodeManagement.trustedCertUrlBeanFactory", "Trusted Certificate", 1, -1);
    }

    @Override
    public TrustedCertUrl make() {
        return new TrustedCertUrl();
    }
}
