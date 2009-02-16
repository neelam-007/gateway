package com.l7tech.security.cert;

/**
 * Represents a high level activity the Gateway performs, that can be mapped to a set of required key usage flags
 * by a KeyUsagePolicy.
 */
public enum KeyUsageActivity {
    /**
     * Signing some XML with the certified public key, intended to be verified using the private key.
     * <b>NOTE:</b> This normally never happens.  A situation that appears to warrant use of this Activity deserves careful second thought.
     */
    signXml,

    /** Verifying the signature of some signed XML using the public key from this cert. */
    verifyXml,

    /** Encrypting XML with the public key from this certificate, encrypted for its corresponding private key. */
    encryptXml,

    /**
     * Decrypting some XML encrypted for the certified public key, encrypted with the corresponding private key.
     * <b>NOTE:</b> This normally never happens.  A situation that appears to warrant use of this Activity deserves careful second thought.
     */
    decryptXml,

    /** In an outgoing SSL connection, trust this remote SSL server cert.  (Passing this check is necessary for trust, but not sufficient.) */
    sslServerRemote,

    /** In an incoming SSL connection, trust this client cert presented in response to our client challenge.  (Passing this check is necessary for trust, but not sufficient.) */
    sslClientRemote,

    /** Allow this certificate's public key to be used to verify the signature of other certs. */
    verifyClientCert,

    /** Trust a Certificate Revocation List signed by this cert.   (Passing this check is necessary for trust, but not sufficient.) */
    verifyCrl,
}
