package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.xml.XpathExpression;

/**
 * Class <code>ElementSecurity</code> contains the security propertiesfor an XML document
 * portion (document element).
 *
 * It contains properties such as:
 * <ul>
 * <li>XPath expression (may be <code>null</code>). It is evaluating context responsibility
 *     to determine the exact behaviour and if it is required or not. For example <code>null</code>
 *     value may be interpreted as the whole document signing.
 * <li>required, indicating wheter the security element is required or not
 * <li> the encryption toggle, whether the document is signed only or signed and encrypted
 * <li>the encryption cypher name
 * <li>the encryption key length
 * </ul>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ElementSecurity {
    /**
     * The default cipher to use when encryption is enabled.
     */
    public static final String DEFAULT_CIPHER = "AES";

    /**
     * The default key length in bits to use when encryption is enabled.
     */
    public static final int DEFAULT_KEYBITS = 128;

    private XpathExpression xpathExpression;
    private boolean required;
    private boolean encryption;
    private String cipher = DEFAULT_CIPHER;
    private int keyLength = DEFAULT_KEYBITS;

    /**
     * Default constructor. mainly for xml serialization
     */
    public ElementSecurity() {
    }

    /**
     * Full constructor, instantiate the instances with all properties.
     *
     * @param xpathExpression the xpath expression that is evaluated in the context
     *                        may be null
     * @param encryption      whether the element is encrypted or signed only
     * @param cipher          the cipher name for encryption, may not be null if encryption
     *                        is requested
     * @param keyLength       the key length, required if the encyption is requested
     */
    public ElementSecurity(XpathExpression xpathExpression, boolean encryption, String cipher, int keyLength) {
        this.encryption = encryption;
        this.cipher = cipher;
        this.keyLength = keyLength;
        this.xpathExpression = xpathExpression;
    }

    /**
     * The xpath expression that is
     *
     * @return the xpath expression, may be null
     */
    public XpathExpression getXpathExpression() {
        return xpathExpression;
    }

    /**
     * Set the xpath expression
     *
     * @param xpathExpression the xpath expression value (or null)
     */
    public void setXpathExpression(XpathExpression xpathExpression) {
        this.xpathExpression = xpathExpression;
    }

    /**
     * Return the required value. This is context dependent property that
     * typically indicates whether the security element is required or not.
     *
     * @return true if required, false otherwise
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Set the required property for this security element.
     *
     * @param required the boolean required value
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * This property describes whether or not the encryption is set on the assertion.
     * The exact meaning of the encryption depends of the implementing class.
     * <p/>
     * In the body should be encrypted as opposed to only signed
     *
     * @return whether the encryption is used or not
     */
    public boolean isEncryption() {
        return encryption;
    }

    /**
     * Enable or disable the encryption on the assertion. The exact meaning of the
     * encryption flag depends on the implementing class.
     *
     * @param b toggle the encryption on the assertion
     */
    public void setEncryption(boolean b) {
        encryption = b;
    }

    /**
     * Set the cipher to use when encryption is enabled.
     *
     * @param cipherName the cipher to use, ie "AES"
     */
    public void setCipher(String cipherName) {
        this.cipher = cipherName;
    }

    /**
     * Get the cipher to use when encryption is enabled.
     *
     * @return the cipher to use, ie "AES"
     */
    public String getCipher() {
        return cipher;
    }

    /**
     * Set the symmetric key length to use when encryption is enabled.
     *
     * @param keyBits the size of the key in bits, ie 128
     */
    public void setKeyLength(int keyBits) {
        this.keyLength = keyBits;
    }

    /**
     * Get the symmetric key length to use when encryption is enabled.
     *
     * @return the size of the key in bits, ie 128
     */
    public int getKeyLength() {
        return keyLength;
    }

}
