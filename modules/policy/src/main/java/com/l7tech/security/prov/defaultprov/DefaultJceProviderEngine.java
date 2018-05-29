package com.l7tech.security.prov.defaultprov;

import com.l7tech.security.prov.DelegatingJceProvider;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.bc.BouncyCastleJceProviderEngine;
import com.l7tech.security.prov.ccj.CryptoComplyJceProviderEngine;
import com.l7tech.util.ConfigFactory;
import com.safelogic.cryptocomply.jce.provider.SLProvider;

import java.security.Provider;
import java.security.Security;
import java.util.logging.Logger;

/**
 * Default JCE Provider Engine initializes CryptoComplyJava in FIPS mode and Bouncy Castle in Non-FIPS mode.
 */
public class DefaultJceProviderEngine extends DelegatingJceProvider {
    private static final Logger logger = Logger.getLogger(DefaultJceProviderEngine.class.getName());
    private static final String PROP_FIPS = "com.l7tech.security.fips.enabled";
    private static final String PROP_PERMAFIPS = "com.l7tech.security.fips.alwaysEnabled";
    private static final String PROP_TLS_PROV = "com.l7tech.security.tlsProvider";

    private final Provider provider;

    public DefaultJceProviderEngine() {
        super(findDelegate());
        if (delegate instanceof CryptoComplyJceProviderEngine) {
            provider = CryptoComplyJceProviderEngine.getProvider();
        } else {
            provider = BouncyCastleJceProviderEngine.getProvider();
        }
    }

    private static JceProvider findDelegate() {
        final boolean FIPS = ConfigFactory.getBooleanProperty( PROP_FIPS, false );
        final boolean permafips = ConfigFactory.getBooleanProperty(PROP_PERMAFIPS, false);
        if (FIPS || permafips) {
            logger.info("Initializing CryptoComply library in FIPS 140 mode");
            return new CryptoComplyJceProviderEngine();
        } else {
            logger.info("Initializing Bouncy Castle library in Non-FIPS mode");
            return new BouncyCastleJceProviderEngine();
        }
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
            return provider;
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
        if (SERVICE_TLS10.equalsIgnoreCase(service))
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