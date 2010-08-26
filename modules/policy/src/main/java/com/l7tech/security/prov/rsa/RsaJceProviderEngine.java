package com.l7tech.security.prov.rsa;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A JceProvider engine for RSA Crypto-J 4.0 FIPS 140.
 */
public class RsaJceProviderEngine extends JceProvider {
    private static final Logger logger = Logger.getLogger(RsaJceProviderEngine.class.getName());

    private static final String PROP_FIPS = "com.l7tech.security.fips.enabled";
    private static final String PROP_PERMAFIPS = "com.l7tech.security.fips.alwaysEnabled";
    private static final String PROP_TLS_PROV = "com.l7tech.security.tlsProvider";

    private static final boolean FIPS = SyspropUtil.getBoolean(PROP_FIPS, false);

    private static final CryptoJWrapper cryptoj;
    private static final Provider PROVIDER;
    private static final Provider PKCS12_PROVIDER;
    private static final Provider TLS10_PROVIDER;
    private static final Provider TLS12_PROVIDER;
    private static final String cryptojVersion;

    private static final String defaultSecureRandom;

    static {
        try {
            final boolean permafips = SyspropUtil.getBoolean(PROP_PERMAFIPS, false);
            if (FIPS || permafips) {
                logger.info("Initializing RSA library in FIPS 140 mode");
                cryptoj = new CryptoJWrapper(true);
                cryptoj.setMode(cryptoj.FIPS140_SSL_ECC_MODE);
                PROVIDER = cryptoj.provider;
                Security.insertProviderAt(PROVIDER, 1);
                if (!cryptoj.isInFIPS140Mode()) {
                    logger.severe("RSA library failed to initialize in FIPS 140 mode");
                    throw new RuntimeException("RSA JCE Provider is supposed to be in FIPS mode but is not");
                }
                defaultSecureRandom = "FIPS186PRNG"; // Use faster but still-FIPS-certified PRNG for things like TLS

            } else {
                logger.info("Initializing RSA library in non-FIPS 140 mode");
                cryptoj = new CryptoJWrapper(false);
                if (cryptoj.isFIPS140Compliant())
                    cryptoj.setMode(cryptoj.NON_FIPS140_MODE);
                PROVIDER = cryptoj.provider;
                Security.addProvider(PROVIDER);
                defaultSecureRandom = null;
            }
            cryptojVersion = String.valueOf(cryptoj.getVersion());
            logger.info("RSA Crypto-J version: " + cryptojVersion);
            PKCS12_PROVIDER = findPkcs12Provider(cryptoj.provider.getClass().getClassLoader());
            TLS10_PROVIDER = findTls10Provider(cryptojVersion, cryptoj.getClass().getClassLoader());
            TLS12_PROVIDER = findTls12Provider(cryptojVersion);
            maybeChangeDefaultTlsProvider(cryptojVersion);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Unable to set FIPS 140 mode (with SSL and ECC): " + ExceptionUtils.getMessage(e), e);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize software cryptography provider: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static void maybeChangeDefaultTlsProvider(String cryptojVersion) throws InstantiationException, IllegalAccessException {
        if (null != SyspropUtil.getProperty(PROP_TLS_PROV)) {
            logger.fine("Leaving default TLS provider unchanged");
            return;
        }

        // See if SSL-J is available
        if (!isCompatibleWithSslJ51(cryptojVersion)) {
            logger.info("TLS 1.2 not enabled.  The TLS 1.2 provider is not compatible with the current cryptography provider.");
            return;
        }

        Provider jsseProvider = findRsaJsseProvider(cryptojVersion);
        if (jsseProvider == null) {
            logger.fine("TLS 1.2 not enabled.  The TLS 1.2 provider is not available.");
            return;
        }

        Provider sunjsse = Security.getProvider("SunJSSE");
        Security.insertProviderAt(jsseProvider, 1);
        if (sunjsse != null) {
            // Move SunJSSE to the end.  We can't unregister it completely because we still need to use it
            // and, because of how it is implemented, it is non-functional unless registered as a security provider.
            Security.removeProvider("SunJSSE");
            Security.addProvider(sunjsse);
        }
        logger.info("Registered " + jsseProvider.getName() + " " + jsseProvider.getVersion() + " as default JSSE provider");
    }

    /*
     * Attempt to locate a provider of KeyStore.PKCS12 that can be used should the current provider (SunJSSE)
     * be removed (perhaps in order to replace it with RsaJsse as default TLS provider).
     */
    private static Provider findPkcs12Provider(ClassLoader cryptojClassLoader) throws IllegalAccessException, InstantiationException {
        // First see if SunJSSE is available
        Provider prov = findSunJsseProvider(cryptojClassLoader);
        if (prov != null) {
            logger.fine("Using Sun PKCS#12 implementation");
            return prov;
        }

        // Check for Crypto-J as fallback measure
        prov = cryptoj.getPkcs12Provider();
        if (prov != null) {
            logger.fine("Using RSA PKCS#12 implementation");
            return prov;
        }

        return null;
    }

    private static Provider findTls10Provider(String cryptojVersion, ClassLoader cryptojClassLoader) throws InstantiationException, IllegalAccessException {
        String provName = SyspropUtil.getProperty(PROP_TLS_PROV);
        if (provName != null && provName.trim().length() > 0) {
            Provider prov = Security.getProvider(provName);
            if (prov != null) {
                logger.info("Using specified TLS 1.0 provider: " + provName);
                return prov;
            }
        }

        // Prefer SunJSSE as TLS 1.0 provider since it currently has cleaner TrustManager behavior
        Provider prov = findSunJsseProvider(cryptojClassLoader);
        if (prov != null) {
            logger.fine("Using SunJSSE as TLS 1.0 provider");
            return prov;
        }

        // If SunJSSE not available, try to use SSL-J (if available and compatible with current Crypto-J version)
        prov = findRsaJsseProvider(cryptojVersion);
        if (prov != null) {
            logger.fine("Using RsaJsse as TLS 1.0 provider");
            return prov;
        }

        return null;
    }

    private static Provider findTls12Provider(String cryptojVersion) throws InstantiationException, IllegalAccessException {
        String provName = SyspropUtil.getProperty(PROP_TLS_PROV);
        if (provName != null && provName.trim().length() > 0) {
            Provider prov = Security.getProvider(provName);
            if (prov != null) {
                logger.info("Using specified TLS 1.2 provider: " + provName);
                return prov;
            }
        }

        // We can only provide TLS 1.2 if SSL-J is availble in classpath and compatible with current Crypto-J version
        Provider prov = findRsaJsseProvider(cryptojVersion);
        if (prov != null) {
            logger.fine("Using RsaJsse as TLS 1.2 provider");
            return prov;
        }

        return null;
    }

    private static Provider findSunJsseProvider(ClassLoader cryptojClassLoader) throws  IllegalAccessException, InstantiationException {
        try {
            return (Provider) cryptojClassLoader.loadClass("com.sun.net.ssl.internal.ssl.Provider").newInstance();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Provider findRsaJsseProvider(String cryptojVersion) throws  IllegalAccessException, InstantiationException {
        if (!isCompatibleWithSslJ51(cryptojVersion))
            return null;
        return cryptoj.getJsseProvider();
    }


    @Override
    public boolean isFips140ModeEnabled() {
        try {
            return cryptoj.isInFIPS140Mode();
        } catch (Exception e) {
            throw new RuntimeException("Unable to check whether provider is in FIPS mode: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public boolean isComponentCompatible(String componentName) {
        return COMPONENT_RSA_SSLJ_5_1_1.equals(componentName) ? isCompatibleWithSslJ51(cryptojVersion) : super.isComponentCompatible(componentName);
    }

    private static boolean isCompatibleWithSslJ51(String ver) {
        // SSL-J 5.1 ships with Crypto-J 4.1, but appears to also work with Crypto-J 4.1.0.1.
        // It is known NOT to be compatible with available earlier FIPS-certified versions (4.0 and 4.01).
        return "4.101".equals(ver) || "4.1".equals(ver);
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
    public SecureRandom newSecureRandom() {
        try {
            return defaultSecureRandom == null ? super.newSecureRandom() : SecureRandom.getInstance(defaultSecureRandom);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.WARNING, "Unable to initialize preferred SecureRandom implementation of " + defaultSecureRandom + "; will use system default instead: " + ExceptionUtils.getMessage(e), e);
            return super.newSecureRandom();
        }
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

    @Override
    public Provider getProviderFor(String service) {
        if (PKCS12_PROVIDER != null && SERVICE_KEYSTORE_PKCS12.equalsIgnoreCase(service))
            return PKCS12_PROVIDER;
        if (TLS10_PROVIDER != null && SERVICE_TLS10.equals(service))
            return TLS10_PROVIDER;
        if (TLS12_PROVIDER != null && SERVICE_TLS12.equals(service))
            return TLS12_PROVIDER;
        return super.getProviderFor(service);
    }
}
