package com.l7tech.security.prov.generic;

import com.l7tech.security.prov.JceProvider;

/**
 * "Generic" JceProviderEngine.  Assumes that the underlying JDK is already configured.
 */
public class GenericJceProviderEngine extends JceProvider {
    @Override
    public String getDisplayName() {
        return "Generic";
    }

    @Override
    public String getRsaNoPaddingCipherName() {
        return "RSA/NONE/NoPadding";
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
