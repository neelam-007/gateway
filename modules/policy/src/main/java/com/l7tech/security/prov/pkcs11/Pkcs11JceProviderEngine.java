package com.l7tech.security.prov.pkcs11;

import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.SyspropUtil;

import java.io.File;
import java.security.*;
import java.util.logging.Logger;

/**
 * A JceProviderEngine that uses the first available PKCS11 provider configured in the JVM, or else
 * adds a new one using the SunPKCS11 config file from the com.l7tech.server.pkcs11ConfigFile system
 * property (defaults to "/opt/SecureSpan/Appliance/etc/pkcs11_linux.cfg").
 */
public class Pkcs11JceProviderEngine extends JceProvider {
    protected static final Logger logger = Logger.getLogger(Pkcs11JceProviderEngine.class.getName());

    /**
     * A config file names a particular PKCS#11 library and may set additional settings for it.  Here's a
     * minimal example of one:
     * <pre>
     *    name=MyAccelerator
     *    library=/usr/local/lib/pkcs11/PKCS11_API.so64
     * </pre>
     */
    public static final String PROPERTY_CONFIG_PATH = "com.l7tech.server.pkcs11ConfigFile";
    private static final String DEFAULT_CONFIG_PATH = "/opt/SecureSpan/Appliance/etc/pkcs11_linux.cfg";
    private static final String CONFIG_PATH = SyspropUtil.getString(PROPERTY_CONFIG_PATH, DEFAULT_CONFIG_PATH);
    private static final Provider PROVIDER;

    static {
        Provider found = null;
        Provider[] provs = Security.getProviders();
        for (Provider prov : provs) {
            if (prov.getName().toLowerCase().startsWith("sunpkcs11")) {
                found = prov;
                logger.info("Using existing preconfigured PKCS#11 security provider " + found.getName());
                break;
            }
        }

        if (found == null) {
            logger.info("No sunpkcs11 security provider found in JVM -- trying to add one using config file " + CONFIG_PATH);
            File cf = new File(CONFIG_PATH);
            if (!cf.exists() || !cf.isFile() || !cf.canRead())
                throw new IllegalStateException("No SunPKCS11 security provider registered, and no config file to create one (checked for " + CONFIG_PATH + ")");

            throw new IllegalStateException("No sunpkcs11 security provider found in JVM");
        }

        PROVIDER = found;
    }

    @Override
    public String getDisplayName() {
        return PROVIDER.toString();
    }

    @Override
    public boolean isFips140ModeEnabled() {
        // We only support configuring the SCA 6000 in FIPS mode.
        return true;
    }

    @Override
    public CertificateRequest makeCsr( String username, KeyPair keyPair ) throws SignatureException, InvalidKeyException {
        throw new UnsupportedOperationException("Pkcs11JceProviderEngine is unable to create new Certificate Signing Request using PKCS#11 KeyPair: Unsupported operation");
    }

    @Override
    public String getRsaNoPaddingCipherName() {
        return "RSA/NONE/NoPadding";
    }

    @Override
    public String getRsaOaepPaddingCipherName() {
        return "RSA/ECB/OAEPPadding";
    }

    @Override
    public String getRsaPkcs1PaddingCipherName() {
        return "RSA/ECB/PKCS1Padding";
    }
}
