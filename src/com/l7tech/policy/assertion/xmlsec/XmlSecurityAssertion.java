package com.l7tech.policy.assertion.xmlsec;

/**
 * The <code>XmlSecurityAssertion</code> that sets the encryption toggle.
 * <p>
 * For example for the xml digital signature the xml encryption may be set.
 * In the case of <code>SamlAssertion</code>
 */
public interface XmlSecurityAssertion {
    /** The default cipher to use when encryption is enabled. */
    public static final String DEFAULT_CIPHER = "AES";

    /** The default key length in bits to use when encryption is enabled. */
    public static final int DEFAULT_KEYBITS = 128;

    /**
     * This property describes whether or not the encryption is set on the assertion.
     * The exact meaning of the encryption depends of the implementing class.
     *
     * In the body should be encrypted as opposed to only signed
     *
     * @return whether the encryption is used or not
     */
    boolean isEncryption();

    /**
     * Enable or disable the encryption on the assertion. The exact meaning of the
     * encryption flag depends on the implementing class.
     *
     * @param b toggle the encryption on the assertion
     */
    void setEncryption(boolean b);

    /**
     * Set the cipher to use when encryption is enabled.
     * @param cipherName the cipher to use, ie "AES"
     */
    void setCipher(String cipherName);

    /**
     * Get the cipher to use when encryption is enabled.
     * @return the cipher to use, ie "AES"
     */
    String getCipher();

    /**
     * Set the symmetric key length to use when encryption is enabled.
     * @param keyBits the size of the key in bits, ie 128
     */
    void setKeyLength(int keyBits);

    /**
     * Get the symmetric key length to use when encryption is enabled.
     * @return the size of the key in bits, ie 128
     */
    int getKeyLength();
}
