package com.l7tech.security.xml;

import java.util.*;

import static com.l7tech.security.xml.SupportedDigestMethods.*;

/**
 * All supported crypto algorithms used in the gateway for XML/message based security.  Along with each algorithm
 * identifier is the corresponding {@link SupportedDigestMethods}.  These two enums should be the only place in the code
 * where we hardcode the signing/hash algorithm identifier strings.
 *
 * User: vchan
 *
 * @see com.l7tech.security.xml.SupportedDigestMethods
 */
public enum SupportedSignatureMethods {

    /** RSA with SHA-1 (defaults from XSS4J) */
    RSA_SHA1("RSA", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", SHA1),
    /** DSA with SHA-1 (defaults from XSS4J) */
    DSA_SHA1("DSA", "http://www.w3.org/2000/09/xmldsig#dsa-sha1", SHA1),
    /** HMAC with SHA-1 (defaults from XSS4J) */
    HMAC_SHA1("SecretKey", "http://www.w3.org/2000/09/xmldsig#hmac-sha1", SHA1),
    /** RSA with SHA-256 extension*/
    RSA_SHA256("RSA", "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", SHA256),
    /** RSA with SHA-384 extension*/
    RSA_SHA384("RSA", "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384", SHA384),
    /** RSA with SHA-512 extension*/
    RSA_SHA512("RSA", "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", SHA512),
    /** ECDSA with SHA-1 (Suite-B crypto support) */
    ECDSA_SHA1("EC", "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1", SHA1),
    /** ECDSA with SHA-256 (Suite-B crypto support) */
    ECDSA_SHA256("EC", "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256", SHA256),
    /** ECDSA with SHA-384 (Suite-B crypto support) */
    // rfc#4051 stated URI is actually from "dsigmore" rather than "dsig": http://www.w3.org/2001/04/xmldsig-more#sha384
    ECDSA_SHA384("EC", "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384", SHA384),
    /** ECDSA with SHA-512 (Suite-B crypto support) */
    ECDSA_SHA512("EC", "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512", SHA512)
    ;

    /** The key algorithm name, ie "RSA" or "EC", for public or private keys, or "SecretKey" for secret keys. */
    private String keyAlg;
    /** The signing algorithm identifier */
    private final String algorithmIdentifier;
    /** The corresponding message digest method */
    private final SupportedDigestMethods digestMethod;
    /** This UI display name. */
    private final String displayName;

    /**
     * Private constructor.
     *
     * @param algId the algorithm id
     * @param msgDigestId the message digest id
     */
    private SupportedSignatureMethods(String keyAlg, String algId, SupportedDigestMethods digestMethod) {
        this.keyAlg = keyAlg;
        this.algorithmIdentifier = algId;
        this.digestMethod = digestMethod;
        this.displayName = keyAlg + " / " + digestMethod.getCanonicalName();
    }

    /**
     * @return the key algorithm name for signature methods that sign with private keys, ie "EC", "DSA", or "RSA"; or the string "SecretKey" for HMAC signature methods
     */
    public String getKeyAlgorithmName() {
        return keyAlg;
    }

    /**
     * @return the message digest algorithm name, ie "SHA-1" or "SHA-256" etc.  Never null.
     */
    public String getDigestAlgorithmName() {
        return digestMethod.getCanonicalName();
    }

    /**
     * Returns the algorithm identifier
     *
     * @return string for the algorithm identifier
     */
    public String getAlgorithmIdentifier() {
        return algorithmIdentifier;
    }

    /**
     * Returns the corresponding message digest identifier associated with the signature algorithm.
     *
     * @return string
     */
    public String getMessageDigestIdentifier() {
        return digestMethod.getIdentifier();
    }

    /**
     * @return a display name to use for UI purposes.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the first SupportedSignatureMethod that has the specified display name.
     *
     * @param displayName the display name to look up.
     * @return the first matching signature method, or null if no match.
     */
    public static SupportedSignatureMethods fromDisplayName(String displayName) {
        for (SupportedSignatureMethods sm : values()) {
            if (sm.getDisplayName().equals(displayName))
                    return sm;
        }
        return null;
    }

    /**
     * Returns SupportedSignatureMethod that matches the specified key algorithm name and message digest algorithm, or null.
     *
     * @param keyAlgorithm  the key algorithm, ie "EC", "RSA", or "DSA" for a public or private key; or "SecretKey" for a SecretKey.  Required.
     * @param messageDigestAlgorithm  the message digest algorithm, ie "SHA-1" or "SHA-512".  Required.
     * @return the matching SupportedSignatureMethod, or null if there is no match.
     */
    public static SupportedSignatureMethods fromKeyAndMessageDigest(String keyAlgorithm, String messageDigestAlgorithm) {
        if (keyAlgorithm == null) throw new IllegalArgumentException("keyAlgorithm is required");
        if (messageDigestAlgorithm == null) throw new IllegalArgumentException("messageDigestAlgorithm is required");
        for (SupportedSignatureMethods m : SupportedSignatureMethods.values()) {
            if (keyAlgorithm.equalsIgnoreCase(m.keyAlg) && messageDigestAlgorithm.equalsIgnoreCase(m.getDigestAlgorithmName()))
                return m;
        }
        return null;
    }

    /**
     * Returns a SupportedSignatureMethod that matches the specified key algorithm name.  This will use the smallest
     * supported hash size; this is less secure but means we won't later try to use (for example) an SHA-512 signature method
     * with a puny 768 bit RSA key or 256 bit EC key.
     *
     * @param keyAlgorithm  the key algorithm, ie "EC", "RSA", or "DSA" for a public or private key; or "SecretKey" for a SecretKey.  Required.
     * @return a matching SupportedSignatureMethod, or null if there is no match.
     */
    public static SupportedSignatureMethods fromKeyAlg(String keyAlgorithm) {
        if (keyAlgorithm == null) throw new IllegalArgumentException("keyAlgorithm is required");
        for (SupportedSignatureMethods m : SupportedSignatureMethods.values()) {
            if (keyAlgorithm.equalsIgnoreCase(m.keyAlg) && m.getDigestAlgorithmName() != null)
                return m;
        }
        return null;
    }

    /**
     * Returns the SupportedSignatureMethod that matches the algorithm argument value.
     *
     * @param algorithm
     * @return
     */
    public static SupportedSignatureMethods fromSignatureAlgorithm(String algorithm) {
        for (SupportedSignatureMethods m : SupportedSignatureMethods.values()) {
            if (m.getAlgorithmIdentifier().equals(algorithm))
                return m;
        }
        throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
    }

    /**
     * Get a list of signature methods that it would be reasonable to enable by default.
     *
     * @return an EnumSet of signature methods that were considered safe to use at the time this code was last updated.
     */
    public static EnumSet<SupportedSignatureMethods> getDefaultMethods() {
        return EnumSet.of(
                RSA_SHA1,
                DSA_SHA1,
                RSA_SHA256,
                ECDSA_SHA1,
                ECDSA_SHA256,
                ECDSA_SHA384,
                ECDSA_SHA512);
    }
}
