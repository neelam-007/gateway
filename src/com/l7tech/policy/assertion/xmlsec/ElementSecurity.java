package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.XpathExpression;

import java.io.Serializable;

/**
 * Class <code>ElementSecurity</code> contains the security properties for an XML document
 * portion (document element).
 * <p/>
 * It contains properties such as:
 * <ul>
 * <li>XPath expression (may be <code>null</code>) that selects the document element to process.
 * It is evaluating context responsibility to determine the exact behaviour and if it is required
 * or not. For example <code>null</code> value may be interpreted as the whole document signing.
 * <li>precondition XPath expression (may be <code>null</code>). If present it is evaluated prior
 * to evaluating the xpath expression. It helps with cinditional 'if-then' scenarios, such as
 * signing an element for a SOAP given operation.
 * <li> the encryption toggle, whether the document is signed only or signed and encrypted
 * <li>the encryption cypher name
 * <li>the encryption key length
 * </ul>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ElementSecurity implements Serializable {
    /**
     * The default cipher to use when encryption is enabled.
     */
    public static final String DEFAULT_CIPHER = "AES";

    /**
     * The default key length in bits to use when encryption is enabled.
     */
    public static final int DEFAULT_KEYBITS = 128;

    private XpathExpression xPath;
    private XpathExpression preconditionXPath;
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
     * @param xpath        the xpath expression that is evaluated in the context
     *                     may be null
     * @param precondition the preconditino xpath, may be null
     * @param encryption   whether the element is encrypted or signed only
     * @param cipher       the cipher name for encryption, may not be null if encryption
     *                     is requested
     * @param keyLength    the key length, required if the encyption is requested
     */
    public ElementSecurity(XpathExpression xpath, XpathExpression precondition, boolean encryption, String cipher, int keyLength) {
        this.encryption = encryption;
        this.cipher = cipher;
        this.keyLength = keyLength;
        this.xPath = xpath;
        this.preconditionXPath = precondition;
    }

    /**
     * The xpath expression that selects the element to apply the
     * security (sign/encryption)
     *
     * @return the xpath expression, may be null
     */
    public XpathExpression getxPath() {
        return xPath;
    }

    /**
     * Set the new xpath expression
     *
     * @param xPath the xpath expression value (or null)
     */
    public void setxPath(XpathExpression xPath) {
        this.xPath = xPath;
    }

    /**
     * The xpath expression that is a conditional for selecting the
     * element to apply the security (sign/encryption)
     *
     * @return the xpath conditional expression, may be null
     */
    public XpathExpression getPreconditionXPath() {
        return preconditionXPath;
    }

    /**
     * Set the new precondition xpath expression
     *
     * @param preconditionXPath the xpath expression value (or null)
     */
    public void setPreconditionXPath(XpathExpression preconditionXPath) {
        this.preconditionXPath = preconditionXPath;
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

    /**
     * Test if this element XPath expression selects the SOAP envelope.
     * <p/>
     *
     * @return true if the element selects the SOAP envelope, false otherwise
     */
    public static boolean isEnvelope(ElementSecurity element) {
        if (element == null || element.getxPath() == null) return false;

        return SoapUtil.SOAP_ENVELOPE_XPATH.equals(element.getxPath().getExpression());
    }

    /**
     * Test if this element XPath expression selects the SOAP Body.
     * <p/>
     *
     * @return true if the element selects the SOAP Body, false otherwise
     */
    public static boolean isBody(ElementSecurity element) {
        if (element == null || element.getxPath() == null) return false;
        return SoapUtil.SOAP_BODY_XPATH.equals(element.getxPath().getExpression());
    }

}
