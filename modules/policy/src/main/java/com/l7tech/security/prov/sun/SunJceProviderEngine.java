package com.l7tech.security.prov.sun;

import com.l7tech.security.prov.JceProvider;
import com.sun.crypto.provider.SunJCE;

import java.security.Provider;
import java.security.Security;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 */
public class SunJceProviderEngine extends JceProvider {
    private final Provider PROVIDER = new SunJCE();

    public SunJceProviderEngine() {
        Security.addProvider(PROVIDER);
    }

    @Override
    public String getDisplayName() {
        return PROVIDER.toString();
    }

    @Override
    public String getRsaNoPaddingCipherName() {
        return "RSA/ECB/NoPadding";
    }

    @Override
    public String getRsaOaepPaddingCipherName() {
        return "RSA/ECB/OAEPPadding";
    }

    @Override
    public String getRsaPkcs1PaddingCipherName() {
        return "RSA/ECB/PKCS1Padding";
    }
}