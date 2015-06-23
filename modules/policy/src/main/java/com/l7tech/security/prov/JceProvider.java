package com.l7tech.security.prov;

import com.l7tech.security.cert.BouncyCastleCertUtils;
import com.l7tech.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLContext;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Option.first;

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

    // System property to disable fallback to default JceProvider if overridden JceProvider fails to initialize
    public static final String DISABLE_FALLBACK_PROPERTY = "com.l7tech.common.security.disableJceProviderFallback";

    public static final int DEFAULT_RSA_KEYSIZE = ConfigFactory.getIntProperty( "com.l7tech.security.prov.defaultRsaKeySize", 2048 );
    public static final String DEFAULT_CSR_SIG_ALG = ConfigFactory.getProperty( "com.l7tech.security.prov.defaultCsrSigAlg", "SHA1withRSA" );

    // Available drivers
    public static final String BC_ENGINE = "com.l7tech.security.prov.bc.BouncyCastleJceProviderEngine";
    public static final String SUN_ENGINE = "com.l7tech.security.prov.sun.SunJceProviderEngine";
    public static final String PKCS11_ENGINE = "com.l7tech.security.prov.pkcs11.Pkcs11JceProviderEngine";
    public static final String LUNA_ENGINE = "com.l7tech.security.prov.luna.LunaJceProviderEngine";
    public static final String NCIPHER_ENGINE = "com.l7tech.security.prov.ncipher.NcipherJceProviderEngine";
    public static final String RSA_ENGINE = "com.l7tech.security.prov.rsa.RsaJceProviderEngine";
    public static final String GENERIC_ENGINE = "com.l7tech.security.prov.generic.GenericJceProviderEngine";

    // Old driver class names
    private static final String OLD_BC_ENGINE = "com.l7tech.common.security.prov.bc.BouncyCastleJceProviderEngine";
    private static final String OLD_SUN_ENGINE = "com.l7tech.common.security.prov.sun.SunJceProviderEngine";
    private static final String OLD_PKCS11_ENGINE = "com.l7tech.common.security.prov.pkcs11.Pkcs11JceProviderEngine";

    /** Recognized service names to pass to {@link #getProviderFor(String)}. */
    public static final String SERVICE_SECURERANDOM_DEFAULT = "SecureRandom.DEFAULT";
    public static final String SERVICE_PBE_WITH_SHA1_AND_DESEDE = "Cipher.PBEWithSHA1AndDESede";
    public static final String SERVICE_CERTIFICATE_GENERATOR = "Signature.BouncyCastleCertificateGenerator";
    public static final String SERVICE_CSR_SIGNING = "Signature.BouncyCastleCsrSigner";
    public static final String SERVICE_SIGNATURE_RSA_PRIVATE_KEY = "Signature.NONEwithRSA.privateKey"; // any RSA signing using a private key that might be from an HSM
    public static final String SERVICE_SIGNATURE_ECDSA = "Signature.NONEwithECDSA"; // any ECDSA signing or signature verification
    public static final String SERVICE_KEYSTORE_PKCS12 = "KeyStore.PKCS12";
    public static final String SERVICE_TLS10 = "SSLContext.TLSv1";   // Not the real service name, but lets us distinguish
    public static final String SERVICE_TLS12 = "SSLContext.TLSv1.2"; // SunJSSE from RsaJsse on the basis of which one (currently) support TLSv1.2
    public static final String SERVICE_DIFFIE_HELLMAN_SOFTWARE = "KeyPairGenerator.DiffieHellmanSoftware";

    private static final String SYSPROP_SSL_DEBUG = "javax.net.debug";

    /* Recognized component names to pass to {@link #isComponentCompatible(String)}. */

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
        put("ncipher", NCIPHER_ENGINE);
        put("generic", GENERIC_ENGINE);
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
        private static final JceProvider engine = createInitialEngine();

        private static String getEngineClassname() {
            String overrideEngineName = ConfigFactory.getProperty( ENGINE_OVERRIDE_PROPERTY, null );
            if (overrideEngineName != null && overrideEngineName.trim().length() > 0) {
                String overrideClassname = OVERRIDE_MAP.get(overrideEngineName.trim().toLowerCase());
                if (overrideClassname != null)
                    return overrideClassname;
                logger.log(Level.WARNING, "Ignoring unknown " + ENGINE_OVERRIDE_PROPERTY + ": " + overrideEngineName);
            }

            return mapEngine( ConfigFactory.getProperty( ENGINE_PROPERTY, DEFAULT_ENGINE ) );
        }

        private static JceProvider createInitialEngine() {
            final JceProvider jceProvider = doCreateEngine();

            // Use preferred SecureRandom in place of default SecureRandom
            RandomUtil.setFactory(new Functions.Nullary<SecureRandom>() {
                @Override
                public SecureRandom call() {
                    return jceProvider.newSecureRandom();
                }
            });

            return jceProvider;
        }

        private static JceProvider doCreateEngine() {
            try {
                return (JceProvider) Class.forName(ENGINE_NAME).getConstructors()[0].newInstance();
            } catch (Throwable t) {
                String msg = "Requested JceProvider could not be instantiated: " + ExceptionUtils.getMessage(t);
                logger.log(Level.SEVERE, msg, t);
                final boolean nofallback = ConfigFactory.getBooleanProperty( DISABLE_FALLBACK_PROPERTY, false );
                if (nofallback)
                    throw new RuntimeException(t);
                try {
                    if (DEFAULT_ENGINE.equals(ENGINE_NAME) || nofallback)
                        throw new RuntimeException(t);
                    logger.log(Level.SEVERE, "Attempting to fallback to default JceProvider engine: " + DEFAULT_ENGINE);
                    return (JceProvider) Class.forName(DEFAULT_ENGINE).getConstructors()[0].newInstance();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private static JceProvider getEngine() {
            return engine;
        }
    }

    private final AtomicReference<SecureRandom> sharedSecureRandom = new AtomicReference<SecureRandom>();

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
     * Check whether the JceProvider is in FIPS 140 compliant mode.
     *
     * @return true if the current JceProvider is capable of and is operating in FIPS 140 compliant mode.
     *         false if it is either not capable of a FIPS 140 compliant mode or is not operating in it.
     */
    public boolean isFips140ModeEnabled() {
        return false;
    }

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
     * @param random a SecureRandom instance with which to initialize the KeyPairGenerator, or null to use the default.  Optional.
     * @return the generated key pair.  Never null.
     * @throws NoSuchAlgorithmException  if ECC is not currently available.
     * @throws InvalidAlgorithmParameterException if the specified curve name is unrecognized.
     */
    public KeyPair generateEcKeyPair(String curveName, SecureRandom random) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        try {
            return tryGenerateEcKeyPair(curveName, random);
        } catch (InvalidAlgorithmParameterException e) {
            // Try synonyms, if any
            Set<String> syns = JceUtil.getCurveNameSynonyms( curveName, false );
            for (String syn : syns) {
                try {
                    return tryGenerateEcKeyPair(syn, random);
                } catch (InvalidAlgorithmParameterException e2) {
                    // Ignore
                }
            }
            // Oh well, we tried
            throw e;
        }
    }

    protected KeyPair tryGenerateEcKeyPair(String curveName, SecureRandom random) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator kpg = getKeyPairGenerator("EC", getProviderFor("KeyPairGenerator.EC"));
        ECGenParameterSpec spec = new ECGenParameterSpec(curveName);
        if (random != null) {
            kpg.initialize(spec, random);
        } else {
            kpg.initialize(spec);
        }
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
        return BouncyCastleCertUtils.makeCertificateRequest(username, keyPair, getProviderFor(SERVICE_CSR_SIGNING));
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
    public String getRsaNoPaddingCipherName() {
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
     * Get a GcmCipher implementation configured to use AES-GCM.
     *
     * @return a GcmCipher implementation.
     * @throws NoSuchProviderException   if there is a configuration problem.  Shouldn't happen.
     * @throws NoSuchAlgorithmException  if this provider is unable to deliver an appropriately-configured AES-GCM implementation.  Will happen unless provider supports GCM mode.
     * @throws NoSuchPaddingException    if this provider is unable to deliver an appropriately-configured AES-GCM implementation.
     */
    public GcmCipher getAesGcmCipherWrapper() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return new DefaultGcmCipher(getAesGcmCipher());
    }

    /**
     * Get an implementation of AES configured to use Galois/Counter Mode.
     *
     * @return a Cipher implementation.  Never null.
     * @throws NoSuchProviderException   if there is a configuration problem.  Shouldn't happen.
     * @throws NoSuchAlgorithmException  if this provider is unable to deliver an appropriately-configured AES-GCM implementation.  Will happen unless provider supports GCM mode.
     * @throws NoSuchPaddingException    if this provider is unable to deliver an appropriately-configured AES-GCM implementation.
     */
    public Cipher getAesGcmCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return getCipher(getAesGcmCipherName(), getProviderFor("Cipher.AES"));
    }

    public String getAesGcmCipherName() {
        return "AES/GCM/NoPadding";
    }

    /**
     * Generate AlgorithmParameters for initialization of an AES/GCM cipher obtained from the current symmetric crypto
     * provider.
     * <p/>
     * The number of bytes in the auth tag may be specified.  Additional authenticated (but non-encrypted) data is not supported:
     * the generated parameters will always specify zero bytes of authenticated data.
     * <p/>
     * This method always throws NoSuchAlgorithmException, but specific providers may override it if they support AES-GCM.
     *
     * @param authTagLenBytes authentication tag length in bytes.  Must be between 12 and 16 inclusive.
     * @param iv the IV to use for encryption or decryption.  Required.  Must be exactly 12 bytes long.
     *        for encryption, this must be a new iv filled with random bytes.
     *        for decryption, this iv must come from the sender of the message (typically this will be the first 12 bytes of the ciphertext)
     * @return an AlgorithmParameterSpec instance suitable for initializing an AES/GCM cipher from the symmetric provider in ENCRYPT_MODE or DECRYPT_MODE
     * @throws InvalidAlgorithmParameterException if AlgorithParameters cannot be generated using the specified settings.
     * @throws NoSuchAlgorithmException if AES-GCM mode is not supported by this JceProvider.
     */
    public AlgorithmParameterSpec generateAesGcmParameterSpec(int authTagLenBytes, @NotNull byte[] iv) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        try {
            // Create a JDK 7 GCMParameterSpec, if the class is available, and hope that the current AES provider will know what to make of it
            return (AlgorithmParameterSpec)Class.forName("javax.crypto.spec.GCMParameterSpec").getConstructor(int.class, byte[].class).newInstance(authTagLenBytes * 8, iv);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("AES-GCM not supported when using the current crypto provider");
        }
    }

    /**
     * Get an implementation of the specified MessageDigest.
     *
     * @param messageDigest the algorithm to get, ie "SHA-384".  Required.
     * @return an implementation.  Never null.
     * @throws NoSuchAlgorithmException if the specified algorithm is invalid or not available from the provider.
     */
    public MessageDigest getMessageDigest(String messageDigest) throws NoSuchAlgorithmException {
        return getMessageDigest(messageDigest, getProviderFor("MessageDigest." + messageDigest));
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
     * @throws KeyStoreException if the specified keystore type is not available.
     */
    public KeyStore getKeyStore(String kstype) throws KeyStoreException {
        return getKeyStore(kstype, getProviderFor("KeyStore." + kstype));
    }

    /**
     * Get a reusable cached instance of the preferred SecureRandom implementation for this JceProvider.
     * <p/>
     * The algorithm may differ from the system-default SecureRandom in cases where the highest-preference Security Provider
     * offers an unsuitable default and cannot be configured differently (Bug #8885).
     * <p/>
     * All callers of this method will share a single synchronized SecureRandom instance.
     * To access a reusable pool of SecureRandom instances, see {@link RandomUtil}.
     *
     * @return a SecureRandom implementation.  Never null.
     */
    public SecureRandom getSecureRandom() {
        SecureRandom ret = sharedSecureRandom.get();
        if (ret == null) {
            sharedSecureRandom.compareAndSet(null, newSecureRandom());
            ret = sharedSecureRandom.get();
        }
        return ret != null ? ret : newSecureRandom();
    }

    /**
     * Create a new instance of the preferred SecureRandom implementation for this JceProvider.
     * <p/>
     * This may differ from the system-default SecureRandom in cases where the highest-preference Security Provider
     * offers an unsuitable default and cannot be configured differently (Bug #8885).
     * 
     * @return a new SecureRandom implementation.  Never null.
     */
    public SecureRandom newSecureRandom() {
        return new SecureRandom();
    }

    /**
     * Get a provider for bulk block ciphers.  This is broken out from getProviderFor() for performance reasons.
     *
     * @return a Provider for block Cipher implementations (AES, DES), or null if the default provider should be used instead.
     */
    public Provider getBlockCipherProvider() {
        return getProviderFor("Cipher.AES");
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
     * Get the preferred Provider for the specified service.
     *
     * @param service the service to get. This should be the name of a "real" service, to get the provider
     *          for a JceProvider "service" name use <code>getProviderFor</code>.
     * @return The preferred provider for the service, or null if no provider is registered for the service.
     * @see #getProviderFor(String)
     */
    public final Provider getPreferredProvider( final String service ) {
        //NOTE: We may want to pre-process the name here, e.g. Ciper.RSA/NONE/NoPadding -> Cipher.RSA
        final Provider sp = getProviderFor( service );
        return sp != null ? sp : first( Security.getProviders( service ) ).toNull();
    }

    /**
     * Get the specified MessageDigest from the specified Provider (which may be null).
     *
     * @param algorithmName  the hash to get.  Required.
     * @param prov the provider to get it from, or null to get it from the current higheset-preference Provider for that hash.
     * @return an implementation of the requested hash.  Never null.
     * @throws NoSuchAlgorithmException if the requested hash is invalid or not available from the specified provider.
     */
    public static MessageDigest getMessageDigest(String algorithmName, Provider prov) throws NoSuchAlgorithmException {
        return prov == null ? MessageDigest.getInstance(algorithmName) : MessageDigest.getInstance(algorithmName, prov);
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

    /**
     * Perform any provider-specific initialization that may be required for the specified newly-created
     * SSLContext.
     * <p/>
     * An example of such initialization is configuring a custom session cache with a more aggressive
     * replacement policy to avoid running out of memory when using SSL-J with the default session cache.
     *
     * @param sslContext an SSLContext to configure.  Required.  Implementors must check to ensure the provided
     *                   context was created by their JSSE provider and must not assume it was.
     */
    public void prepareSslContext( @NotNull SSLContext sslContext ) {
    }

    /**
     * Set debug options for the provider.
     *
     * <p>Any options that are not specified in the given set should not be
     * udpated. A null value for an option means that any existing value for
     * that option should be cleared.</p>
     *
     * <p>The supported names and values are provider specific, a provider
     * should ignore any unrecognised options.</p>
     *
     * @param options The debug options to use (required)
     */
    public void setDebugOptions( final Map<String,String> options ) {
        if ( options.containsKey( "ssl" ) ) {
            final String debugValue = options.get( "ssl" );

            // Forbid the value "help" as this will cause some providers to terminate
            // the JVM
            if ( debugValue == null || debugValue.toLowerCase().contains( "help" ) ) {
                SyspropUtil.clearProperty( SYSPROP_SSL_DEBUG );
            } else {
                SyspropUtil.setProperty( SYSPROP_SSL_DEBUG, debugValue );
            }
        }
    }

    // If you add new methods to this class, don't forget to add them to DelegatingJceProvider
}
