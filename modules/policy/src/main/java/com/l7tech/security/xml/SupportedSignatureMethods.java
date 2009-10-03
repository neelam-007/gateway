package com.l7tech.security.xml;

import java.util.EnumSet;

/**
 * All supported crypto algorithms used in the gateway for XML/message based security.  Along with each algorithm
 * identifier is the corresponding message digest identifier.  This enum should be the only place in the code
 * where we hardcode the signing/hash algorithm identifier strings.
 *
 * User: vchan
 */
public enum SupportedSignatureMethods {

    /** RSA with SHA-1 (defaults from XSS4J) */
    RSA_SHA1("RSA", "SHA-1", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", "http://www.w3.org/2000/09/xmldsig#sha1"),
    /** DSA with SHA-1 (defaults from XSS4J) */
    DSA_SHA1("DSA", "SHA-1", "http://www.w3.org/2000/09/xmldsig#dsa-sha1", "http://www.w3.org/2000/09/xmldsig#sha1"),
    /** HMAC with SHA-1 (defaults from XSS4J) */
    HMAC_SHA1("SecretKey", "SHA-1", "http://www.w3.org/2000/09/xmldsig#hmac-sha1", "http://www.w3.org/2000/09/xmldsig#sha1"),
    /** RSA with SHA-256 extension*/
    RSA_SHA256("RSA", "SHA-256", "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", "http://www.w3.org/2001/04/xmlenc#sha256"),
    /** ECDSA with SHA-1 (Suite-B crypto support) */
    ECDSA_SHA1("EC", "SHA-1", "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1", "http://www.w3.org/2000/09/xmldsig#sha1"),
    /** ECDSA with SHA-256 (Suite-B crypto support) */
    ECDSA_SHA256("EC", "SHA-256", "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256", "http://www.w3.org/2001/04/xmlenc#sha256"),
    /** ECDSA with SHA-384 (Suite-B crypto support) */
    // rfc#4051 stated URI is actually from "dsigmore" rather than "dsig": http://www.w3.org/2001/04/xmldsig-more#sha384
    ECDSA_SHA384("EC", "SHA-384", "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384", "http://www.w3.org/2001/04/xmldsig-more#sha384"),
    /** ECDSA with SHA-512 (Suite-B crypto support) */
    ECDSA_SHA512("EC", "SHA-512", "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512", "http://www.w3.org/2001/04/xmlenc#sha512")
    ;

    /** The key algorithm name, ie "RSA" or "EC", for public or private keys, or "SecretKey" for secret keys. */
    private String keyAlg;
    /** The digest algorithm, ie "SHA-1" or "SHA-256". */
    private String digestAlg;
    /** The signing algorithm identifier */
    private final String algorithmIdentifier;
    /** The corresponding message digest identifier */
    private final String messageDigestIdentifier;
    /** This UI display name. */
    private final String displayName;

    /**
     * Private constructor.
     *
     * @param algId the algorithm id
     * @param msgDigestId the message digest id
     */
    private SupportedSignatureMethods(String keyAlg, String digestAlg, String algId, String msgDigestId) {
        this.keyAlg = keyAlg;
        this.digestAlg = digestAlg;
        this.algorithmIdentifier = algId;
        this.messageDigestIdentifier = msgDigestId;
        this.displayName = keyAlg + " / " + digestAlg;
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
        return digestAlg;
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
        return messageDigestIdentifier;
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
            if (m.keyAlg != null && m.keyAlg.equalsIgnoreCase(keyAlgorithm) && m.digestAlg != null && m.digestAlg.equalsIgnoreCase(messageDigestAlgorithm))
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
