package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
@ProcessesResponse
public class ResponseWssConfidentiality extends XmlSecurityAssertionBase {
    public ResponseWssConfidentiality() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public ResponseWssConfidentiality(XpathExpression xpath) {
        setXpathExpression(xpath);
    }

    /**
     * Get the requested xml encrytpion algorithm. Defaults to http://www.w3.org/2001/04/xmlenc#aes128-cbc
     * @return the encrytion algorithm requested
     */
    public String getXEncAlgorithm() {
        return xencAlgorithm;
    }

    /**
     * Set the xml encryption algorithm.
     * @param xencAlgorithm
     */
    public void setXEncAlgorithm(String xencAlgorithm) {
        if (xencAlgorithm == null) {
            throw new IllegalArgumentException();
        }
        this.xencAlgorithm = xencAlgorithm;
    }

    public String getKeyEncryptionAlgorithm() {
        return xencKeyAlgorithm;
    }

    public void setKeyEncryptionAlgorithm(String keyEncryptionAlgorithm) {
        this.xencKeyAlgorithm = keyEncryptionAlgorithm;
    }

    private String xencAlgorithm = XencUtil.AES_128_CBC;
    private String xencKeyAlgorithm = null;
}
