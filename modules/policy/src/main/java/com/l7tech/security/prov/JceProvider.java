/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Provide a single point where our JCE provider can be altered.
 * @author mike
 * @version 1.0
 */
public abstract class JceProvider {
    public static final String ENGINE_PROPERTY = "com.l7tech.common.security.jceProviderEngine";

    // Available drivers
    public static final String BC_ENGINE = "com.l7tech.security.prov.bc.BouncyCastleJceProviderEngine";
    public static final String RSA_ENGINE = "com.l7tech.security.prov.rsa.RsaJceProviderEngine";
    public static final String NCIPHER_ENGINE = "com.l7tech.security.prov.ncipher.NcipherJceProviderEngine";
    public static final String SUN_ENGINE = "com.l7tech.security.prov.sun.SunJceProviderEngine";
    public static final String LUNA_ENGINE = "com.l7tech.security.prov.luna.LunaJceProviderEngine";
    public static final String PKCS11_ENGINE = "com.l7tech.security.prov.pkcs11.Pkcs11JceProviderEngine";

    // Old driver class names
    private static final String OLD_BC_ENGINE = "com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine";
    private static final String OLD_RSA_ENGINE = "com.l7tech.common.security.prov.rsa.RsaJceProviderEngine";
    private static final String OLD_NCIPHER_ENGINE = "com.l7tech.common.security.prov.ncipher.NcipherJceProviderEngine";
    private static final String OLD_SUN_ENGINE = "com.l7tech.common.security.prov.sun.SunJceProviderEngine";
    private static final String OLD_LUNA_ENGINE = "com.l7tech.common.security.prov.luna.LunaJceProviderEngine";
    private static final String OLD_PKCS11_ENGINE = "com.l7tech.common.security.prov.pkcs11.Pkcs11JceProviderEngine";

    private static final Map<String,String> DRIVER_MAP;
    static {
        Map<String,String> driverMap = new HashMap<String,String>();
        driverMap.put( OLD_BC_ENGINE, BC_ENGINE );
        driverMap.put( OLD_RSA_ENGINE, RSA_ENGINE );
        driverMap.put( OLD_NCIPHER_ENGINE, NCIPHER_ENGINE );
        driverMap.put( OLD_SUN_ENGINE, SUN_ENGINE );
        driverMap.put( OLD_LUNA_ENGINE, LUNA_ENGINE );
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

    public static Cipher getRsaNoPaddingCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
        return Holder.engine.getRsaNoPaddingCipher();
    }

    public static Cipher getRsaOaepPaddingCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
        return Holder.engine.getRsaOaepPaddingCipher();
    }

    public static Cipher getRsaPkcs1PaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Holder.engine.getRsaPkcs1PaddingCipher();
    }

    public static RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType) {
        return Holder.engine.createRsaSignerEngine(keyStorePath, storePass, privateKeyAlias, privateKeyPass, storeType);
    }

    public static KeyPair generateRsaKeyPair() {
        return Holder.engine.generateRsaKeyPair();
    }

    public static KeyPair generateRsaKeyPair(int keysize) {
        return Holder.engine.generateRsaKeyPair(keysize);
    }

    public static CertificateRequest makeCsr(String username, KeyPair keyPair)
                 throws SignatureException, InvalidKeyException, RuntimeException
    {
        return Holder.engine.makeCsr(username, keyPair);
    }
}
