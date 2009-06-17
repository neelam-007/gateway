package com.l7tech.security.xml;

import com.ibm.xml.dsig.SignatureMethod;
import com.ibm.xml.dsig.XSignature;

/**
 * All supported crypto algorithms used in the gateway for XML/message based security.  Along with each algorithm
 * identifier is the corresponding message digest identifier.  This enum should be the only place in the code
 * where we hardcode the signing/hash algorithm identifier strings.
 *
 * User: vchan
 */
public enum SupportedSignatureMethods {

    /** RSA with SHA-1 (defaults from XSS4J) */
    RSA_SHA1(SignatureMethod.RSA, XSignature.SHA1),
    /** DSA with SHA-1 (defaults from XSS4J) */
    DSA_SHA1(SignatureMethod.DSA, XSignature.SHA1),
    /** HMAC with SHA-1 (defaults from XSS4J) */
    HMAC_SHA1(SignatureMethod.HMAC, XSignature.SHA1),
    /** RSA with SHA-256 extension*/
    RSA_SHA256("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", "http://www.w3.org/2001/04/xmlenc#sha256"),
    /** ECDSA with SHA-1 (Suite-B crypto support) */
    ECDSA_SHA1("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1", XSignature.SHA1),
    /** ECDSA with SHA-256 (Suite-B crypto support) */
    ECDSA_SHA256("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256", "http://www.w3.org/2001/04/xmlenc#sha256"),
    /** ECDSA with SHA-384 (Suite-B crypto support) */
    // rfc#4051 stated URI is actually from "dsigmore" rather than "dsig": http://www.w3.org/2001/04/xmldsig-more#sha384
    ECDSA_SHA384("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384", "http://www.w3.org/2001/04/xmldsig-more#sha384"),
    /** ECDSA with SHA-512 (Suite-B crypto support) */
    ECDSA_SHA512("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512", "http://www.w3.org/2001/04/xmlenc#sha512")
    ;

    /** The signing algorithm identifier */
    private final String algorithmIdentifier;
    /** The corresponding message digest identifier */
    private final String messageDigestIdentifier;

    /**
     * Private constructor.
     *
     * @param algId the algorithm id
     * @param msgDigestId the message digest id
     */
    private SupportedSignatureMethods(String algId, String msgDigestId) {
        this.algorithmIdentifier = algId;
        this.messageDigestIdentifier = msgDigestId;
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
}
