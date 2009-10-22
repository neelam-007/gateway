package com.l7tech.security.prov.rsa;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import java.lang.reflect.InvocationTargetException;
import java.security.Provider;
import java.security.Security;
import java.util.logging.Logger;

/**
 * A JceProvider engine for RSA Crypto-J 4.0 FIPS 140.
 */
public class RsaJceProviderEngine extends JceProvider {
    private static final Logger logger = Logger.getLogger(RsaJceProviderEngine.class.getName());

    private static final String PROP_FIPS = "com.l7tech.security.fips.enabled";
    private static final String PROP_PERMAFIPS = "com.l7tech.security.fips.alwaysEnabled";
    private static final String PROP_SUNJSSE_FIPS = "com.l7tech.security.sunjsse.fips.enabled";

    private static final boolean FIPS = SyspropUtil.getBoolean(PROP_FIPS, false);
    private static final boolean SUNJSSE_FIPS = SyspropUtil.getBoolean(PROP_SUNJSSE_FIPS, false);

    private static final CryptoJWrapper cryptoj;
    private static final Provider PROVIDER;

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

                if (SUNJSSE_FIPS) {
                    // If the SunJSSE provider is being used for SSL, replace it with the FIPS version
                    Provider sunjsse = Security.getProvider("SunJSSE");
                    if (sunjsse == null) {
                        logger.info("SunJSSE provider is not installed -- unable to put SunJSSE into FIPS mode (continuing anyway)");
                        // Continue anyway, in case we are running with a non-Sun or specially-configured JVM
                    } else {
                        Provider fipsJsse = (Provider)Class.forName("com.sun.net.ssl.internal.ssl.Provider").getConstructor(Provider.class).newInstance(PROVIDER);
                        Security.removeProvider("SunJSSE");
                        Security.addProvider(fipsJsse);
                        logger.info("Reregistered SunJSSE in FIPS mode using RSA library as provider");
                    }
                }
            } else {
                logger.info("Initializing RSA library in non-FIPS 140 mode");
                cryptoj = new CryptoJWrapper(false);
                if (cryptoj.isFIPS140Compliant())
                    cryptoj.setMode(cryptoj.NON_FIPS140_MODE);
                PROVIDER = cryptoj.provider;
                Security.insertProviderAt(PROVIDER, 1);
            }
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to put SunJSSE SSL library into FIPS mode: " + ExceptionUtils.getMessage(e), e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to put SunJSSE SSL library into FIPS mode: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to put SunJSSE SSL library into FIPS mode: " + ExceptionUtils.getMessage(e), e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to put SunJSSE SSL library into FIPS mode: " + ExceptionUtils.getMessage(e), e);
        } catch (ClassNotFoundException e) {
            // This might happen semi-legitimately if a later Sun JDK removes/renames/changes the com.sun.net.ssl.internal.ssl.Provider class,
            // or if we are running on a non-Sun JDK that nevertheless has a Provider installed named "SunJSSE"
            throw new RuntimeException("Unable to initialize software crypto provider: " + ExceptionUtils.getMessage(e), e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Unable to set FIPS 140 mode (with SSL and ECC): " + ExceptionUtils.getMessage(e), e);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set FIPS 140 mode (with SSL and ECC): " + ExceptionUtils.getMessage(e), e);
        }
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
