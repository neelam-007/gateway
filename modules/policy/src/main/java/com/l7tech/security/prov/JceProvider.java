/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov;

import com.l7tech.util.SyspropUtil;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provide a single point where our JCE provider can be altered.
 * @author mike
 * @version 1.0
 */
public abstract class JceProvider {
    public static final String ENGINE_PROPERTY = "com.l7tech.common.security.jceProviderEngine";

    private static final int DEFAULT_RSA_KEYSIZE = SyspropUtil.getInteger("com.l7tech.security.prov.defaultRsaKeySize", 1024);

    // Available drivers
    public static final String BC_ENGINE = "com.l7tech.security.prov.bc.BouncyCastleJceProviderEngine";
    public static final String SUN_ENGINE = "com.l7tech.security.prov.sun.SunJceProviderEngine";
    public static final String PKCS11_ENGINE = "com.l7tech.security.prov.pkcs11.Pkcs11JceProviderEngine";
    public static final String LUNA_ENGINE = "com.l7tech.security.prov.luna.LunaJceProviderEngine";
    public static final String LUNA_PKCS11_ENGINE = "com.l7tech.security.prov.lunapkcs11.LunaPkcs11JceProviderEngine";
    public static final String CERTICOM_ENGINE = "com.l7tech.security.prov.certicom.CerticomJceProviderEngine";
    public static final String RSA_ENGINE = "com.l7tech.security.prov.rsa.RsaJceProviderEngine";

    // Old driver class names
    private static final String OLD_BC_ENGINE = "com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine";
    private static final String OLD_SUN_ENGINE = "com.l7tech.common.security.prov.sun.SunJceProviderEngine";
    private static final String OLD_PKCS11_ENGINE = "com.l7tech.common.security.prov.pkcs11.Pkcs11JceProviderEngine";

    /** Recognized service name to pass to {@link #getProviderFor(String)}. */
    public static String SERVICE_PBE_WITH_SHA1_AND_DESEDE = "Cipher.PBEWithSHA1AndDESede";


    private static final Map<String,String> DRIVER_MAP;

    static {
        Map<String,String> driverMap = new HashMap<String,String>();
        driverMap.put( OLD_BC_ENGINE, BC_ENGINE );
        driverMap.put( OLD_SUN_ENGINE, SUN_ENGINE );
        driverMap.put( OLD_PKCS11_ENGINE, PKCS11_ENGINE );
        DRIVER_MAP = Collections.unmodifiableMap(driverMap);
    }

    static String mapEngine( final String engineClass ) {
        String mappedClass = DRIVER_MAP.get( engineClass );

        if ( mappedClass == null ) {
            mappedClass = engineClass;
        }

        return mappedClass;
    }

    // Default driver
    private static final String DEFAULT_ENGINE = BC_ENGINE;

    private static class Holder {
        private static final String ENGINE = mapEngine(System.getProperty(ENGINE_PROPERTY, DEFAULT_ENGINE));
        private static final JceProviderEngine engine = createEngine();

        private static JceProviderEngine createEngine() {
            try {
                return (JceProviderEngine) Class.forName(ENGINE).getConstructors()[0].newInstance();
            } catch (Throwable t) {
                throw new RuntimeException("Requested JceProviderEngine could not be instantiated: " + t.getMessage(), t);
            }
        }

        private static JceProviderEngine getEngine() {
            return engine;
        }
    }

    public static void init() {
        // no action should be required
        Holder.getEngine();
    }

    /** @return the JceProviderEngine class name currently being used. */
    public static String getEngineClass() {
        init();
        return Holder.ENGINE;
    }

    public static Provider getSymmetricJceProvider() {
        return Holder.engine.getSymmetricProvider();
    }

    public static Provider getAsymmetricJceProvider() {
        return Holder.engine.getAsymmetricProvider();
    }

    public static Provider getSignatureProvider() {
        return Holder.engine.getSignatureProvider();
    }

    /** @return provider recommended for specified service, or null to recommend asking for service without specifying a provider. */
    public static Provider getProviderFor(String service) throws NoSuchProviderException {
        return Holder.engine.getProviderFor(service);
    }

    public static Cipher getRsaNoPaddingCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
        return Holder.engine.getRsaNoPaddingCipher();
    }

    public static Cipher getRsaOaepPaddingCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
        return Holder.engine.getRsaOaepPaddingCipher();
    }

    public static Cipher getRsaPkcs1PaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Holder.engine.getRsaPkcs1PaddingCipher();
    }

    public static RsaSignerEngine createRsaSignerEngine(PrivateKey caKey, X509Certificate[] caCertChain) {
        return Holder.engine.createRsaSignerEngine(caKey, caCertChain);
    }

    public static KeyPair generateRsaKeyPair() {
        return Holder.engine.generateRsaKeyPair(DEFAULT_RSA_KEYSIZE);
    }

    public static KeyPair generateRsaKeyPair(int keysize) {
        return Holder.engine.generateRsaKeyPair(keysize);
    }

    public static KeyPair generateEcKeyPair(String curveName) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        return Holder.engine.generateEcKeyPair(curveName);
    }

    public static CertificateRequest makeCsr(String username, KeyPair keyPair)
                 throws SignatureException, InvalidKeyException, RuntimeException
    {
        return Holder.engine.makeCsr(username, keyPair);
    }
}
