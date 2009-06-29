package com.l7tech.security.prov.rsa;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ExceptionUtils;
import com.rsa.jsafe.crypto.CryptoJ;
import com.rsa.jsafe.crypto.JSAFE_InvalidUseException;
import com.rsa.jsafe.provider.JsafeJCE;

import java.security.Provider;
import java.security.Security;

/**
 * A JceProvider engine for RSA Crypto-J 4.0 FIPS 140.
 */
public class RsaJceProviderEngine extends JceProvider {
    private static final Provider PROVIDER;

    static {
        try {
            CryptoJ.setMode(CryptoJ.FIPS140_SSL_ECC_MODE);
        } catch (JSAFE_InvalidUseException e) {
            throw new RuntimeException("Unable to set FIPS 140 mode (with SSL and ECC): " + ExceptionUtils.getMessage(e), e);
        }
        PROVIDER = new JsafeJCE();
        Security.insertProviderAt(PROVIDER, 1);
        if (!CryptoJ.isInFIPS140Mode())
            throw new RuntimeException("RSA JCE Provider is supposed to be in FIPS mode but is not");
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
        return "RSA/ECB/OAEPPadding";
    }

    @Override
    protected String getRsaPkcs1PaddingCipherName() {
        return "RSA/ECB/PKCS1Padding";
    }
}
