package com.l7tech.security.prov.ncipher;

import com.l7tech.security.prov.JceProvider;
import com.ncipher.provider.km.nCipherKM;

import java.security.Provider;
import java.security.Security;

/**
 *
 */
public class NcipherJceProviderEngine extends JceProvider {
    static final Provider PROVIDER;

    static {
        if (null == System.getProperty("protect")) {
            // Prefer module-level key protection by default
            System.setProperty("protect", "module");
            if (null == System.getProperty("ignorePassphrase")) {
                System.setProperty("ignorePassphrase", "true");
            }
        }
        PROVIDER = new nCipherKM();
        Security.insertProviderAt(PROVIDER, 0);
    }

    @Override
    public String getDisplayName() {
        return PROVIDER.getName();
    }

    @Override
    protected String getRsaNoPaddingCipherName() {
        return "RSA/ECB/NoPadding";
    }

    @Override
    protected String getRsaOaepPaddingCipherName() {
        return "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    }

    @Override
    protected String getRsaPkcs1PaddingCipherName() {
        return "RSA/ECB/PKCS1Padding";
    }
}
