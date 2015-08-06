package com.l7tech.security.prov.rsa;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.rsa.jsafe.crypto.CryptoJ;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A JceProvider engine for RSA Crypto-J 4.0 FIPS 140.
 */
public class RsaJceProviderEngine extends JceProvider {
    private static final Logger logger = Logger.getLogger(RsaJceProviderEngine.class.getName());

    private static final String PROP_FIPS = "com.l7tech.security.fips.enabled";
    private static final String PROP_PERMAFIPS = "com.l7tech.security.fips.alwaysEnabled";
    private static final String PROP_TLS_PROV = "com.l7tech.security.tlsProvider";
    private static final String PROP_CRYPTOJ_DEFAULT_RANDOM_ALG = "com.rsa.crypto.default.random";
    private static final String PROP_SSLJ_SHARED_SESSION_CACHE = "com.l7tech.security.sslj.sharedSessionCache.enable";

    private static final String PROVIDER_NAME_RSAJSSE = "RsaJsse";

    private static final boolean FIPS = ConfigFactory.getBooleanProperty( PROP_FIPS, false );
    private static final boolean ENABLE_SSLJ_SHARED_SESSION_CACHE = SyspropUtil.getBoolean( PROP_SSLJ_SHARED_SESSION_CACHE, true );

    private static final CryptoJWrapper cryptoj;
    private static final Provider PROVIDER;
    private static final Provider PKCS12_PROVIDER;
    private static final Provider PKIX_PROVIDER;
    private static final Provider TLS10_PROVIDER;
    private static final Provider TLS12_PROVIDER;

    // A GCM IV full of zero bytes, for sanity checking IVs
    private static final byte[] ZERO_IV = new byte[12];

    static {
        try {
            final boolean permafips = ConfigFactory.getBooleanProperty( PROP_PERMAFIPS, false );
            if (FIPS || permafips) {
                logger.info("Initializing RSA library in FIPS 140 mode");
                cryptoj = new CryptoJWrapper(true);
                cryptoj.setMode( CryptoJ.FIPS140_SSL_ECC_MODE );
                PROVIDER = cryptoj.provider;
                Security.insertProviderAt(PROVIDER, 1);
                if (!cryptoj.isInFIPS140Mode()) {
                    logger.severe("RSA library failed to initialize in FIPS 140 mode");
                    throw new RuntimeException("RSA JCE Provider is supposed to be in FIPS mode but is not");
                }

            } else {
                logger.info("Initializing RSA library in non-FIPS 140 mode");
                cryptoj = new CryptoJWrapper(false);
                if (cryptoj.isFIPS140Compliant())
                    cryptoj.setMode( CryptoJ.NON_FIPS140_MODE );
                PROVIDER = cryptoj.provider;
                Security.addProvider(PROVIDER);
            }
            logger.info("RSA Crypto-J version: " + String.valueOf(cryptoj.getVersion()));
            PKCS12_PROVIDER = findPkcs12Provider(cryptoj.provider.getClass().getClassLoader());
            PKIX_PROVIDER = findSunJsseProvider(cryptoj.provider.getClass().getClassLoader());
            TLS10_PROVIDER = findTls10Provider(cryptoj.getClass().getClassLoader());
            TLS12_PROVIDER = findTls12Provider();
            maybeChangeDefaultTlsProvider();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Unable to set FIPS 140 mode (with SSL and ECC): " + ExceptionUtils.getMessage(e), e);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize software cryptography provider: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static void maybeChangeDefaultTlsProvider() throws InstantiationException, IllegalAccessException {
        if ( null != ConfigFactory.getProperty( PROP_TLS_PROV ) ) {
            logger.fine("Leaving default TLS provider unchanged");
            return;
        }

        // See if SSL-J is available
        Provider jsseProvider = findRsaJsseProvider();
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

        return null;
    }

    private static Provider findTls10Provider(ClassLoader cryptojClassLoader) throws InstantiationException, IllegalAccessException {
        String provName = ConfigFactory.getProperty( PROP_TLS_PROV );
        if (provName != null && provName.trim().length() > 0) {
            Provider prov = Security.getProvider(provName);
            if (prov != null) {
                logger.info("Using specified TLS 1.0 provider: " + provName);
                return prov;
            }
        }

        // Prefer SunJSSE as TLS 1.0 provider since it currently has cleaner TrustManager behavior
        Provider prov = findSunJsseProvider( cryptojClassLoader );
        if (prov != null) {
            logger.fine("Using SunJSSE as TLS 1.0 provider");
            return prov;
        }

        // If SunJSSE not available, try to use SSL-J (if available and compatible with current Crypto-J version)
        prov = findRsaJsseProvider();
        if (prov != null) {
            logger.fine("Using RsaJsse as TLS 1.0 provider");
            return prov;
        }

        return null;
    }

    private static Provider findTls12Provider() throws InstantiationException, IllegalAccessException {
        String provName = ConfigFactory.getProperty( PROP_TLS_PROV );
        if (provName != null && provName.trim().length() > 0) {
            Provider prov = Security.getProvider(provName);
            if (prov != null) {
                logger.info("Using specified TLS 1.2 provider: " + provName);
                return prov;
            }
        }

        // We can only provide TLS 1.2 if SSL-J is available in classpath and compatible with current Crypto-J version
        Provider prov = findRsaJsseProvider();
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

    private static Provider findRsaJsseProvider() throws  IllegalAccessException, InstantiationException {
        return cryptoj.getJsseProvider();
    }

    @Override
    public AlgorithmParameterSpec generateAesGcmParameterSpec(int authTagLenBytes, @NotNull byte[] iv) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        if (authTagLenBytes < 12 || authTagLenBytes > 16)
            throw new InvalidAlgorithmParameterException("GCM auth tag length must be between 12 and 16 bytes");
        if (iv.length != 12)
            throw new InvalidAlgorithmParameterException("GCM IV must be exactly 12 bytes long");
        if (Arrays.equals(ZERO_IV, iv))
            throw new InvalidAlgorithmParameterException("GCM IV is entirely zero octets");

        return cryptoj.createGcmParameterSpec(authTagLenBytes, 0, iv);
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
    public String getDisplayName() {
        return PROVIDER.getName();
    }

    @Override
    public Provider getBlockCipherProvider() {
        return PROVIDER;
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
        if ("TrustManagerFactory.PKIX".equals(service))
            return PKIX_PROVIDER;
        return super.getProviderFor(service);
    }

    @Override
    public void prepareSslContext( @NotNull SSLContext sslContext ) {
        if ( ENABLE_SSLJ_SHARED_SESSION_CACHE && PROVIDER_NAME_RSAJSSE.equals( sslContext.getProvider().getName() ) ) {
            logger.fine( "Wiring up SSL-J SSLContext to custom TLS session cache" );
            cryptoj.attachSessionCache( sslContext );
        }

        super.prepareSslContext( sslContext );
    }

    @Override
    public void setDebugOptions( final Map<String, String> options ) {
        super.setDebugOptions( options );
        cryptoj.resetDebug();
    }
}
