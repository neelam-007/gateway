package com.l7tech.security.prov.pkcs11;

import com.l7tech.security.prov.JceProviderEngine;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.l7tech.util.SyspropUtil;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.io.File;
import java.util.logging.Logger;

/**
 * A JceProviderEngine that uses the first available PKCS11 provider configured in the JVM, or else
 * adds a new one using the SunPKCS11 config file from the com.l7tech.server.pkcs11ConfigFile system
 * property (defaults to "/ssg/appliance/pkcs11.cfg").
 */
public class Pkcs11JceProviderEngine implements JceProviderEngine {
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
    private static final String DEFAULT_CONFIG_PATH = "/ssg/appliance/pkcs11.cfg";
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

//            TODO uncomment when nightly build is running on Java 1.6, if we want this JceProviderEngine to be self-installing
//            found = new sun.security.pkcs11.SunPKCS11(CONFIG_PATH);
//            Security.addProvider(found);
            throw new IllegalStateException("No sunpkcs11 security provider found in JVM");
        }

        PROVIDER = found;
    }

    public Provider getAsymmetricProvider() {
        return PROVIDER;
    }

    public Provider getSymmetricProvider() {
        return PROVIDER;
    }

    public RsaSignerEngine createRsaSignerEngine(PrivateKey caKey, X509Certificate[] caCertChain) {
        return new BouncyCastleRsaSignerEngine(caKey, caCertChain[0], PROVIDER.getName());
    }

    public KeyPair generateRsaKeyPair(int keysize) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", PROVIDER.getName());
            kpg.initialize(keysize);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("PKCS11 JCE provider misconfigured: " + e.getMessage(), e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException("PKCS11 JCE provider misconfigured: " + e.getMessage(), e);
        }
    }

    public CertificateRequest makeCsr( String username, KeyPair keyPair ) throws SignatureException, InvalidKeyException {
        throw new UnsupportedOperationException("Pkcs11JceProviderEngine is unable to create new Certificate Signing Request using PKCS#11 KeyPair: Unsupported operation");
    }

    public Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/NONE/NoPadding", PROVIDER.getName());
    }

    public Cipher getRsaOaepPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/ECB/OAEPPadding", PROVIDER.getName());
    }

    public Cipher getRsaPkcs1PaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/ECB/PKCS1Padding", PROVIDER.getName());
    }
}
