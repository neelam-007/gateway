package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

/**
 * Enforces XML digital signature on the entire envelope of the response and maybe XML encryption on the body
 * element of the response.
 *
 * Whether XML encryption is used depends on the property encryption
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 27, 2003<br/>
 * $Id$
 */
public class XmlRequestSecurity extends CredentialSourceAssertion implements XmlSecurityAssertion {
    /**
     * This property describes whether or not the body should be encrypted as opposed to only signed
     */
    public boolean isEncryption() {
        return encryption;
    }

    /**
     * This property describes whether or not the body should be encrypted as opposed to only signed
     */
    public void setEncryption(boolean encryption) {
        this.encryption = encryption;
    }

    /**
     * Set the cipher to use when encryption is enabled.
     * @param cipherName the cipher to use, ie "AES"
     */
    public void setCipher(String cipherName) {
        this.cipherName = cipherName;
    }

    /**
     * Get the cipher to use when encryption is enabled.
     * @return the cipher to use, ie "AES"
     */
    public String getCipher() {
        return cipherName;
    }

    /**
     * Set the symmetric key length to use when encryption is enabled.
     * @param keyBits the size of the key in bits, ie 128
     */
    public void setKeyLength(int keyBits) {
        this.keyBits = keyBits;
    }

    /**
     * Get the symmetric key length to use when encryption is enabled.
     * @return the size of the key in bits, ie 128
     */
    public int getKeyLength() {
        return keyBits;
    }

    private boolean encryption = false;
    private String cipherName = DEFAULT_CIPHER;
    private int keyBits = DEFAULT_KEYBITS;
}
