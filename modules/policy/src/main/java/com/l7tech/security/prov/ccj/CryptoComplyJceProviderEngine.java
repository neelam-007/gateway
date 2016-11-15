package com.l7tech.security.prov.ccj;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.ProviderUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Pair;
import com.safelogic.cryptocomply.jce.provider.SLProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * CryptoComply JceProvider Engine contains CryptoComplyJava in FIPS mode and Bouncy Castle in Non-FIPS mode.
 */
public class CryptoComplyJceProviderEngine extends JceProvider {
    private static final Logger logger = Logger.getLogger(CryptoComplyJceProviderEngine.class.getName());
    private static final String PROP_FIPS = "com.l7tech.security.fips.enabled";
    private static final String PROP_PERMAFIPS = "com.l7tech.security.fips.alwaysEnabled";
    private static final String PROP_TLS_PROV = "com.l7tech.security.tlsProvider";
    private static final String PROP_DISABLE_BLACKLISTED_SERVICES = "com.l7tech.security.prov.ccj.disableServices";
    private static final boolean DISABLE_BLACKLISTED_SERVICES = ConfigFactory.getBooleanProperty(PROP_DISABLE_BLACKLISTED_SERVICES, true);

    private final Provider PROVIDER;

    public CryptoComplyJceProviderEngine() {
        final boolean FIPS = ConfigFactory.getBooleanProperty( PROP_FIPS, false );
        final boolean permafips = ConfigFactory.getBooleanProperty(PROP_PERMAFIPS, false);
        if (FIPS || permafips) {
            logger.info("Initializing CryptoComply library in FIPS 140 mode");
            PROVIDER = new SLProvider();
            Security.removeProvider(SLProvider.PROVIDER_NAME);
            Security.insertProviderAt(PROVIDER, 1);

            if (DISABLE_BLACKLISTED_SERVICES) {
                ProviderUtil.removeService(ProviderUtil.SERVICE_BLACKLIST, PROVIDER);
            }
        } else {
            PROVIDER = new BouncyCastleProvider();
            logger.info("Initializing Bouncy Castle library in Non-FIPS mode");
            Security.addProvider(PROVIDER);
        }
    }

    @Override
    public boolean isFips140ModeEnabled() {
        return PROVIDER.getName().equals(SLProvider.PROVIDER_NAME);
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
    protected String getRsaOaepPaddingCipherName() {
        return "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    }

    @Override
    public Provider getPreferredProvider(String service) {
        if ("Cipher.RSA/ECB/NoPadding".equals(service))
            return PROVIDER;
        return super.getPreferredProvider(service);
    }

    @Override
    protected String getRsaPkcs1PaddingCipherName() {
        return "RSA/ECB/PKCS1Padding";
    }

    @Override
    public Provider getProviderFor(String service) {
        if (SERVICE_KEYSTORE_PKCS12.equalsIgnoreCase(service))
            return new com.sun.net.ssl.internal.ssl.Provider();
        if (SERVICE_TLS10.equals(service))
            return getTls10Provider();
        return super.getProviderFor(service);
    }

    private Provider getSunJsseProvider() {
        return new com.sun.net.ssl.internal.ssl.Provider();
    }

    private Provider getTls10Provider() {
        String provName = ConfigFactory.getProperty( PROP_TLS_PROV );
        if (provName != null && provName.trim().length() > 0) {
            Provider prov = Security.getProvider(provName);
            if (prov != null) {
                logger.info("Using specified TLS 1.0 provider: " + provName);
                return prov;
            }
        }

        // Prefer SunJSSE as TLS 1.0 provider for compatibility with previous behavior
        logger.fine("Using SunJSSE as TLS 1.0 provider");
        return getSunJsseProvider();
    }
}