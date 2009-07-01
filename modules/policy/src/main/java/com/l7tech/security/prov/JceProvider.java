/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov;

import com.l7tech.security.prov.bc.BouncyCastleCertificateRequest;
import com.l7tech.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provide a single point where our Security providers can be configured and queried,
 * and gives us the ability to override the default
 * provider selection, when necessary, for certain operations when using certain providers.
 */
public abstract class JceProvider {
    private static final Logger logger = Logger.getLogger(JceProvider.class.getName());

    // Raw engine classname
    public static final String ENGINE_PROPERTY = "com.l7tech.common.security.jceProviderEngine";

    // Whitelisted engine short name; if set, takes precedence over ENGINE_PROPERTY
    public static final String ENGINE_OVERRIDE_PROPERTY = "com.l7tech.common.security.jceProviderEngineName";

    public static final int DEFAULT_RSA_KEYSIZE = SyspropUtil.getInteger("com.l7tech.security.prov.defaultRsaKeySize", 1024);
    public static final String DEFAULT_CSR_SIG_ALG = SyspropUtil.getString("com.l7tech.security.prov.defaultCsrSigAlg", "SHA1withRSA");

    // Available drivers
    public static final String BC_ENGINE = "com.l7tech.security.prov.bc.BouncyCastleJceProviderEngine";
    public static final String SUN_ENGINE = "com.l7tech.security.prov.sun.SunJceProviderEngine";
    public static final String PKCS11_ENGINE = "com.l7tech.security.prov.pkcs11.Pkcs11JceProviderEngine";
    public static final String LUNA_ENGINE = "com.l7tech.security.prov.luna.LunaJceProviderEngine";
    public static final String RSA_ENGINE = "com.l7tech.security.prov.rsa.RsaJceProviderEngine";

    // Old driver class names
    private static final String OLD_BC_ENGINE = "com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine";
    private static final String OLD_SUN_ENGINE = "com.l7tech.common.security.prov.sun.SunJceProviderEngine";
    private static final String OLD_PKCS11_ENGINE = "com.l7tech.common.security.prov.pkcs11.Pkcs11JceProviderEngine";

    /** Recognized service names to pass to {@link #getProviderFor(String)}. */
    public static final String SERVICE_PBE_WITH_SHA1_AND_DESEDE = "Cipher.PBEWithSHA1AndDESede";
    public static final String SERVICE_CERTIFICATE_GENERATOR = "Signature.BouncyCastleCertificateGenerator";
    public static final String SERVICE_CSR_SIGNING = "Signature.BouncyCastleCsrSigner";

    private static final Map<String,String> DRIVER_MAP;

    static {
        Map<String,String> driverMap = new HashMap<String,String>();
        driverMap.put( OLD_BC_ENGINE, BC_ENGINE );
        driverMap.put( OLD_SUN_ENGINE, SUN_ENGINE );
        driverMap.put( OLD_PKCS11_ENGINE, PKCS11_ENGINE );
        DRIVER_MAP = Collections.unmodifiableMap(driverMap);
    }

    // Engines that will be recognized by short name.
    // We prefer to avoid instantiating classnames read from the DB without a whitelist; this is the whitelist.
    private static final Map<String,String> OVERRIDE_MAP = new HashMap<String,String>(){{
        put("default", DEFAULT_ENGINE);
        put("bc", BC_ENGINE);
        put("luna", LUNA_ENGINE);
        put("rsa", RSA_ENGINE);
    }};

    static String mapEngine( final String engineClass ) {
        if (engineClass == null || engineClass.trim().length() < 1)
            return DEFAULT_ENGINE;

        String mappedClass = DRIVER_MAP.get( engineClass );

        if ( mappedClass == null ) {
            mappedClass = engineClass;
        }

        return mappedClass;
    }

    // Default driver
    private static final String DEFAULT_ENGINE = RSA_ENGINE;

    private static class Holder {
        private static final String ENGINE_NAME = getEngineClassname();
        private static final JceProvider engine = createEngine();

        private static String getEngineClassname() {
            String overrideEngineName = SyspropUtil.getString(ENGINE_OVERRIDE_PROPERTY, null);
            if (overrideEngineName != null && overrideEngineName.trim().length() > 0) {
                String overrideClassname = OVERRIDE_MAP.get(overrideEngineName.trim().toLowerCase());
                if (overrideClassname != null)
                    return overrideClassname;
                logger.log(Level.WARNING, "Ignoring unknown " + ENGINE_OVERRIDE_PROPERTY + ": " + overrideEngineName);
            }

            return mapEngine(SyspropUtil.getString(ENGINE_PROPERTY, DEFAULT_ENGINE));
        }

        private static JceProvider createEngine() {
            try {
                return (JceProvider) Class.forName(ENGINE_NAME).getConstructors()[0].newInstance();
            } catch (Throwable t) {
                String msg = "Requested JceProvider could not be instantiated: " + ExceptionUtils.getMessage(t);
                logger.log(Level.SEVERE, msg, t);
                if (DEFAULT_ENGINE.equals(ENGINE_NAME))
                    throw new RuntimeException(t);
                logger.log(Level.SEVERE, "Attempting to fallback to default JceProvider engine: " + DEFAULT_ENGINE);
                try {
                    return (JceProvider) Class.forName(DEFAULT_ENGINE).getConstructors()[0].newInstance();
                } catch (Throwable e) {
                    throw new RuntimeException("Fallback to default JceProvider engine failed: " + ExceptionUtils.getMessage(e), e);
                }
            }
        }

        private static JceProvider getEngine() {
            return engine;
        }
    }

    public static void init() {
        getInstance();
    }

    /** @return the JceProvider class name currently being used. */
    public static String getEngineClass() {
        return getInstance().getClass().getName();
    }

    public static JceProvider getInstance() {
        return Holder.getEngine();
    }

    /**
     * Get a friendly name to use in log messages. <b>Note: Not necessarily a valid Provider name!</b>
     *
     * @return The name of this JceProvider engine.  This is NOT necessarily a registered Provider name!
     */
    public abstract String getDisplayName();

    /**
     * Create an RsaSignerEngine that uses the current crypto API.
     * This can be used to sign certificates.
     *
     * @param caKey       CA private key for signing.  Required.
     * @param caCertChain CA cert chain for signing.  Required.
     * @return an RsaSignerEngine that can be used to sign certificates.  Never null.
     */
    public RsaSignerEngine createRsaSignerEngine(PrivateKey caKey, X509Certificate[] caCertChain) {
        return new BouncyCastleRsaSignerEngine(caKey, caCertChain[0], getProviderFor(SERVICE_CERTIFICATE_GENERATOR));
    }

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider with the default RSA key size.
     *
     * @return a new RSA key pair.  Never null.
     */
    public KeyPair generateRsaKeyPair() {
        return generateRsaKeyPair(DEFAULT_RSA_KEYSIZE);
    }

    /**
     * Generate an RSA public key / private key pair using the current Crypto provider with the specified key size.
     * @param keybits  desired RSA key size in bits, ie 1024.
     * @return a new RSA KeyPair instance with the specified key size.
     */
    public KeyPair generateRsaKeyPair(int keybits) {
        try {
            KeyPairGenerator kpg = getKeyPairGenerator("RSA");
            kpg.initialize(keybits);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No KeyPairGenerator.RSA available; likely a misconfiguration: " + e.getMessage(), e);
        }
    }

    /**
     * Generate an ECC public key / private key pair using the current Crypto provider with the specified curve name.
     *
     * @param curveName  curve name to use, ie "P-384".  Required.
     * @return the generated key pair.  Never null.
     * @throws NoSuchAlgorithmException  if ECC is not currently available.
     * @throws InvalidAlgorithmParameterException if the specified curve name is unrecognized.
     */
    public KeyPair generateEcKeyPair(String curveName) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator kpg = getKeyPairGenerator("EC", getProviderFor("KeyPairGenerator.EC"));
        kpg.initialize(new ECGenParameterSpec(curveName));
        return kpg.generateKeyPair();
    }

    /**
     * Generate a CertificateRequest using the current Crypto provider.
     *
     * @param username  the username, ie "lyonsm"
     * @param keyPair  the public and private keys
     * @return a new PKCS#10 CertificateRequest instance.  Never null.
     * @throws java.security.InvalidKeyException  if the specified key is unsuitable
     * @throws java.security.SignatureException   if there is a problem signing the CSR
     */
    public CertificateRequest makeCsr( String username, KeyPair keyPair ) throws SignatureException, InvalidKeyException {
        return staticMakeCsr( username, keyPair, getProviderFor(SERVICE_CSR_SIGNING) );
    }

    /**
     * Get an implementation of RSA configured to work in raw mode, with padding disabled.
     *
     * @return an RSA Cipher implementation.  Never null.
     * @throws NoSuchProviderException   if there is a configuration problem.  Shouldn't happen.
     * @throws NoSuchAlgorithmException  if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     * @throws NoSuchPaddingException    if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     */
    public Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return getCipher(getRsaNoPaddingCipherName(), getProviderFor("Cipher.RSA"));
    }

    /** @return name to request to get RSA in ECB mode with no padding, or null to disable. */
    protected String getRsaNoPaddingCipherName() {
        return "RSA/NONE/NoPadding";
    }

    /**
     * Get an implementation of RSA configured to work in raw mode, with OAEP padding.
     *
     * @return an RSA Cipher implementation.  Never null.
     * @throws NoSuchProviderException   if there is a configuration problem.  Shouldn't happen.
     * @throws NoSuchAlgorithmException  if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     * @throws NoSuchPaddingException    if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     */
    public Cipher getRsaOaepPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return getCipher(getRsaOaepPaddingCipherName(), getProviderFor("Cipher.RSA"));
    }

    protected String getRsaOaepPaddingCipherName() {
        return "RSA/NONE/OAEPPadding";
    }

    /**
     * Get an implementation of RSA configured to work in raw mode, with PKCS#1 version 1.5 padding.
     *
     * @return an RSA Cipher implementation.  Never null.
     * @throws NoSuchProviderException   if there is a configuration problem.  Shouldn't happen.
     * @throws NoSuchAlgorithmException  if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     * @throws NoSuchPaddingException    if this provider is unable to deliver an appropriately-configured RSA implementation.  Shouldn't happen.
     */
    public Cipher getRsaPkcs1PaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return getCipher(getRsaPkcs1PaddingCipherName(), getProviderFor("Cipher.RSA"));
    }

    protected String getRsaPkcs1PaddingCipherName() {
        return "RSA/NONE/PKCS1Padding";
    }

    /**
     * Obtain a Signature instance from the current JceProvider.
     *
     * @param alg  the signature algorithm, ie "SHA1withRSA".  Required.
     * @return a new Signature instance for the requested algorithm.  Never null.
     * @throws NoSuchAlgorithmException if the requested algorithm name is invalid or not available from the current provider.
     */
    public Signature getSignature(String alg) throws NoSuchAlgorithmException {
        return getSignature(alg, getProviderFor("Signature." + alg));
    }

    /**
     * Obtain a KeyPairGenerator from the current JceProvider.
     *
     * @param algorithm the key pair type, ie "RSA" or "EC".  Required.
     * @return a new KeyPairGenerator instance for the requested algorithm.  Never null.
     * @throws NoSuchAlgorithmException if the requested algorithm name is invalid or not available from the current provider.
     */
    public KeyPairGenerator getKeyPairGenerator(String algorithm) throws NoSuchAlgorithmException {
        return getKeyPairGenerator(algorithm, getProviderFor("KeyPairGenerator." + algorithm));
    }

    /**
     * Obtain a KeyFactory from the current JceProvider.
     *
     * @param algorithm the algorithm, ie "EC" or "AES".  Required.
     * @return a new KeyFactory isntance for the requested algorithm.  Never null.
     * @throws NoSuchAlgorithmException if the requested algorithm name is invalid or not available from the current provider.
     */
    public KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        return getKeyFactory(algorithm, getProviderFor("KeyFactory." + algorithm));
    }

    /**
     * Obtain a keystore implementation from the current JceProvider.
     *
     * @param kstype the keystore type, ie "PKCS12".  Required.
     * @return an implementation of the requested keystore.  Never null.
     * @thorws KeyStoreException if the specified keystore type is not available.
     */
    public KeyStore getKeyStore(String kstype) throws KeyStoreException {
        return getKeyStore(kstype, getProviderFor("KeyStore." + kstype));
    }

    /**
     * Get a Provider appropriate for the specified service.  See JceProvider for a list of recognized service
     * names.
     *
     * @param service the service to get.  Uses the name from JceProvider rather than the "real" name, which may be
     *          different if the current engine names it differently.
     * @return a Provider for the service, or null if the default provider should be used instead.
     */
    public Provider getProviderFor(String service) {
        return null;
    }

    /**
     * Generate a CertificateRequest using the specified Crypto provider.
     *
     * @param username  the username to put in the cert
     * @param keyPair the public and private keys
     * @param provider provider to use for crypto operations, or null to use best preferences.
     * @return a new CertificateRequest instance.  Never null.
     * @throws java.security.InvalidKeyException  if a CSR cannot be created using the specified keypair
     * @throws java.security.SignatureException   if the CSR cannot be signed
     */
    public static CertificateRequest staticMakeCsr(String username, KeyPair keyPair, Provider provider ) throws InvalidKeyException, SignatureException {
        X509Name subject = new X509Name("cn=" + username);
        ASN1Set attrs = null;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Generate request
        try {
            PKCS10CertificationRequest certReq = new PKCS10CertificationRequest(DEFAULT_CSR_SIG_ALG, subject, publicKey, attrs, privateKey, provider == null ? null : provider.getName());
            return new BouncyCastleCertificateRequest(certReq, publicKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Get the specified Cipher from the specified Provider (which may be null).
     *
     * @param cipherName  the transformation, ie "RSA/NONE/PKCS1Padding".  Required.
     * @param prov  the provider to get it from, or null to get it from the current highest-preference Provider for that algorithm.
     * @return an implementation of the requested Cipher algorithm.  Never null.
     * @throws NoSuchAlgorithmException if the requested algorithm name is invalid or not available from the specified provider.
     * @throws NoSuchPaddingException if the requested cipher padding scheme isn't available.
     */
    public static Cipher getCipher(String cipherName, Provider prov) throws NoSuchAlgorithmException, NoSuchPaddingException {
        return prov == null ? Cipher.getInstance(cipherName) : Cipher.getInstance(cipherName, prov);
    }

    /**
     * Get the specified KeyFactory from the specified Provider (which may be null).
     *
     * @param alg the algorithm, ie "RSA" or "EC".  Required.
     * @param prov the provider to get it from, or null to get it from the current highest-preference Provider for that algorithm.
     * @return an implementation of the requested KeyFactory.  Never null.
     * @throws NoSuchAlgorithmException if the requested algorithm name is invalid or not available from the specified provider.
     */
    public static KeyFactory getKeyFactory(String alg, Provider prov) throws NoSuchAlgorithmException {
        return prov == null ? KeyFactory.getInstance(alg) : KeyFactory.getInstance(alg, prov);
    }

    /**
     * Get the specified Signature algorithm from the specified Provider (which may be null).
     *
     * @param sigName  the signature alg, ie "SHA1withRSA".  Required.
     * @param prov   the provider to get it from, or null to get it from the current highest-preference Provider for that algorithm.
     * @return an implementation of the requested Signature algorithm.  Never null.
     * @throws NoSuchAlgorithmException if the requested algorithm name is invalid or not available from the specified provider.
     */
    public static Signature getSignature(String sigName, Provider prov) throws NoSuchAlgorithmException {
        return prov == null ? Signature.getInstance(sigName) : Signature.getInstance(sigName, prov);
    }

    /**
     * Get the specified KeyPairGenerator algorithm from the specified Provider (which may be null).
     *
     * @param alg  the algorithm, ie "RSA" or "EC".  Required.
     * @param prov the provider to get it from, or null to get it from the current highest-preference Provider for that algorithm.
     * @return an implementation of the requested KeyPairGenerator.  Never null.
     * @throws NoSuchAlgorithmException if the requested algorithm name is invalid or not available from the specified provider.
     */
    public static KeyPairGenerator getKeyPairGenerator(String alg, Provider prov) throws NoSuchAlgorithmException {
        return prov == null ? KeyPairGenerator.getInstance(alg) : KeyPairGenerator.getInstance(alg, prov);
    }

    /**
     * Get the specified KeyStore from the specified Provider (which may be null).
     *
     * @param kstype the store type, ie "PKCS12".  Required.
     * @param prov the provider to get it from, or null to get it from the current highest-preference Provider for that service.
     * @return an implementation of the requested KeyStore.  Never null.
     * @throws KeyStoreException if the specified keystore type is invalid or not available from the specified provider.
     */
    private static KeyStore getKeyStore(String kstype, Provider prov) throws KeyStoreException {
        return prov == null ? KeyStore.getInstance(kstype) : KeyStore.getInstance(kstype, prov);
    }
}
