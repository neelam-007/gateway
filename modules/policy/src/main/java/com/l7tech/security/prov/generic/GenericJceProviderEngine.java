package com.l7tech.security.prov.generic;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.SyspropUtil;

/**
 * "Generic" JceProviderEngine.  Assumes that the underlying JDK is already configured.
 */
public class GenericJceProviderEngine extends JceProvider {
    private final String rsaNoPaddingCipherName;
    private final String rsaOaepPaddingCipherName;
    private final String rsaPkcs1PaddingCipherName;

    public GenericJceProviderEngine() {
        rsaNoPaddingCipherName = SyspropUtil.getString("com.l7tech.common.security.prov.generic.rsaNoPaddingCipherName", "RSA/NONE/NoPadding");
        rsaOaepPaddingCipherName = SyspropUtil.getString("com.l7tech.common.security.prov.generic.rsaOaepPaddingCipherName", "RSA/ECB/OAEPPadding");
        rsaPkcs1PaddingCipherName = SyspropUtil.getString("com.l7tech.common.security.prov.generic.rsaPkcs1PaddingCipherName", "RSA/ECB/PKCS1Padding");
    }

    @Override
    public String getDisplayName() {
        return "Generic";
    }

    @Override
    public String getRsaNoPaddingCipherName() {
        return rsaNoPaddingCipherName;
    }

    @Override
    public String getRsaOaepPaddingCipherName() {
        return rsaOaepPaddingCipherName;
    }

    @Override
    public String getRsaPkcs1PaddingCipherName() {
        return rsaPkcs1PaddingCipherName;
    }
}
