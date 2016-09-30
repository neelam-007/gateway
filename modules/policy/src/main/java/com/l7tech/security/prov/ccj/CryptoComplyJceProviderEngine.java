package com.l7tech.security.prov.ccj;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ConfigFactory;
import com.safelogic.cryptocomply.jcajce.provider.CryptoComplyFipsProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.logging.Logger;

/**
 * CryptoComply JceProvider Engine contains CryptoComplyJava in FIPS mode and Bouncy Castle in Non-FIPS mode.
 */
public class CryptoComplyJceProviderEngine extends JceProvider {
    private static final Logger logger = Logger.getLogger(CryptoComplyJceProviderEngine.class.getName());
    private static final String PROP_FIPS = "com.l7tech.security.fips.enabled";
    private static final String PROP_PERMAFIPS = "com.l7tech.security.fips.alwaysEnabled";

    public final Provider PROVIDER;

    public CryptoComplyJceProviderEngine() {
        final boolean FIPS = ConfigFactory.getBooleanProperty( PROP_FIPS, false );
        final boolean permafips = ConfigFactory.getBooleanProperty(PROP_PERMAFIPS, false);
        if (FIPS || permafips) {
            logger.info("Initializing CryptoComply library in FIPS 140 mode");
            PROVIDER = new CryptoComplyFipsProvider();
            Security.insertProviderAt(PROVIDER, 1);
        } else {
            PROVIDER = new BouncyCastleProvider();
            logger.info("Initializing Bouncy Castle library in Non-FIPS mode");
            Security.addProvider(PROVIDER);
        }
    }

    @Override
    public boolean isFips140ModeEnabled() {
        return PROVIDER.getName().equals(CryptoComplyFipsProvider.PROVIDER_NAME);
    }

    @Override
    public String getDisplayName() {
        return PROVIDER.getName();
    }

    @Override
    public String getRsaNoPaddingCipherName() {
        return "RSA/ECB/NoPadding";
    }

    @Override
    public Provider getPreferredProvider(String service) {
        if ("Cipher.RSA/ECB/NoPadding".equals(service))
            return PROVIDER;
        return super.getPreferredProvider(service);
    }

    @Override
    public SecretKey prepareSecretKeyForPBEWithSHA1AndDESede(@NotNull final Cipher cipher, @NotNull final SecretKey secretKey) {
        // If the provider is CCJ, then modify key bytes of the secret key
        if (CryptoComplyFipsProvider.PROVIDER_NAME.equals(cipher.getProvider() == null? null : cipher.getProvider().getName()))  {
            final byte[] keyBytes = secretKey.getEncoded();
            final byte[] modifiedBytes = new byte[keyBytes.length * 2 + 2];

            for (int i = 0; i != keyBytes.length; i++) {
                modifiedBytes[ 1 + (i * 2)] = (byte)(keyBytes[i] & 0x7f);
            }

            return new SecretKeySpec(modifiedBytes, "PBEWithSHA1AndDESede" );
        } else {
            return super.prepareSecretKeyForPBEWithSHA1AndDESede(cipher, secretKey);
        }
    }
}
