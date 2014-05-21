package com.l7tech.policy.assertion.ext.security;

/**
 * Use this service create a signer.
 */
public interface SignerServices {

    /** A special key ID that always refers to the Gateway's current default SSL key, regardless of its alias. */
    static final String KEY_ID_SSL = "XX:SSL";

    /**
     * Create a signer that will use the specified Gateway private key. Returns null if the specified Gateway key is not found.
     *
     * @param keyId a string identifying a Gateway private key keystore ID and key alias, eg "-1:SSL", with
     *              a keystore ID of "-1" indicating to search all keystores for the specified key alias;
     *              or the special key ID {@link #KEY_ID_SSL}.
     *
     * @return a Signer that will sign with the specified key, or null if the specified key is not found. The signer can be used multiple times, from multiple threads.
     * @throws SignerException if there is an error
     */
    Signer createSigner(String keyId) throws SignerException;
}