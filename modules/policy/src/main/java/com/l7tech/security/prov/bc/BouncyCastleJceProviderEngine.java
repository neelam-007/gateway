/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.bc;

import com.l7tech.security.prov.JceProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;

/**
 * BouncyCastle-specific JCE provider engine.
 */
public class BouncyCastleJceProviderEngine extends JceProvider {
    protected final Provider PROVIDER = new BouncyCastleProvider();

    public BouncyCastleJceProviderEngine() {
        Security.addProvider(PROVIDER);
    }

    @Override
    protected Provider getDefaultProvider() {
        return PROVIDER;
    }
}