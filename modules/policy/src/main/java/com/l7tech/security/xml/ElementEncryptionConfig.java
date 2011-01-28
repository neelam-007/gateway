package com.l7tech.security.xml;

/**
 * Holds settings related to how an XML element should be encrypted.
 */
public class ElementEncryptionConfig {
    private final boolean encryptContentsOnly;

    /**
     * Create an encrypted element configuration that will encrypt the content only.
     */
    public ElementEncryptionConfig() {
        this(true);
    }

    /**
     * Create an encrypted element configuration with the specified settings for the content-only flag.
     *
     * @param encryptContentsOnly true to encrypt contents but leave the element open and close tags unencrypted.
     *                            false to replace the entire element with an EncryptedData element.
     */
    public ElementEncryptionConfig(boolean encryptContentsOnly) {
        this.encryptContentsOnly = encryptContentsOnly;
    }

    /**
     * @return true if only the contents of the elment should be encrypted.   False if the open and close tags should be encrypted as well.
     */
    public boolean isEncryptContentsOnly() {
        return encryptContentsOnly;
    }
}
