package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.xml.xpath.XpathExpression;
import com.l7tech.common.security.xml.XencUtil;
import com.l7tech.policy.assertion.PrivateKeyable;

/**
 * Enforces the XML security on the message elements or entire message
 * 
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
public class RequestWssConfidentiality extends XmlSecurityAssertionBase implements PrivateKeyable {
    public RequestWssConfidentiality() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public RequestWssConfidentiality(XpathExpression xpath) {
        setXpathExpression(xpath);
    }

    /**
     * Get the requested xml encrytpion algorithm. Defaults to http://www.w3.org/2001/04/xmlenc#aes128-cbc
     * @return the encrytion algorithm requested
     */
    public String getXEncAlgorithm() {
        return xEncAlgorithm;
    }

    /**
     * Set the xml encryption algorithm.
     * @param xEncAlgorithm
     */
    public void setXEncAlgorithm(String xEncAlgorithm) {
        if (xEncAlgorithm == null) {
            throw new IllegalArgumentException();
        }
        this.xEncAlgorithm = xEncAlgorithm;
    }

    public String getKeyEncryptionAlgorithm() {
        return xencKeyAlgorithm;
    }
                    
    public void setKeyEncryptionAlgorithm(String keyEncryptionAlgorithm) {
        this.xencKeyAlgorithm = keyEncryptionAlgorithm;
    }

    private String xEncAlgorithm = XencUtil.AES_128_CBC;
    private String xencKeyAlgorithm = null;
    private boolean usesDefaultKeyStore = true;
    private long nonDefaultKeystoreId;
    private String keyId;

    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    public void setUsesDefaultKeyStore(boolean usesDefault) {
        this.usesDefaultKeyStore = usesDefault;
    }

    public long getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    public void setNonDefaultKeystoreId(long nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    public String getKeyAlias() {
        return keyId;
    }

    public void setKeyAlias(String keyid) {
        this.keyId = keyid;
    }
}
