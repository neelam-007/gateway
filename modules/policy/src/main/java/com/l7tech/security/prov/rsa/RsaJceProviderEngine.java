package com.l7tech.security.prov.rsa;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.rsa.jsafe.crypto.CryptoJ;
import com.rsa.jsafe.crypto.JSAFE_InvalidUseException;
import com.rsa.jsafe.provider.JsafeJCE;

import java.security.Provider;
import java.security.Security;
import java.util.logging.Logger;

/**
 * A JceProvider engine for RSA Crypto-J 4.0 FIPS 140.
 */
public class RsaJceProviderEngine extends JceProvider {
    private static final Logger logger = Logger.getLogger(RsaJceProviderEngine.class.getName());

    private static final boolean FIPS = SyspropUtil.getBoolean("com.l7tech.security.fips.enabled", false);

    private static final Provider PROVIDER;

    static {
        try {
            if (FIPS) {
                logger.info("Initializing RSA library in FIPS 140 mode");
                CryptoJ.setMode(CryptoJ.FIPS140_SSL_ECC_MODE);
                PROVIDER = new JsafeJCE();
                Security.insertProviderAt(PROVIDER, 1);
                if (!CryptoJ.isInFIPS140Mode()) {
                    logger.severe("RSA library failed to initialize in FIPS 140 mode");
                    throw new RuntimeException("RSA JCE Provider is supposed to be in FIPS mode but is not");
                }
            } else {
                logger.info("Initializing RSA library in non-FIPS 140 mode");
                if (CryptoJ.isFIPS140Compliant())
                    CryptoJ.setMode(CryptoJ.NON_FIPS140_MODE);
                PROVIDER = new JsafeJCE();
                Security.insertProviderAt(PROVIDER, 1);
            }
        } catch (JSAFE_InvalidUseException e) {
            throw new RuntimeException("Unable to set FIPS 140 mode (with SSL and ECC): " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public String getDisplayName() {
        return PROVIDER.getName();
    }

    @Override
    public Provider getBlockCipherProvider() {
        return PROVIDER;
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
