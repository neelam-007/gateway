package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.security.xml.XencUtil;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
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
    public String getXencAlgorithm() {
        return xencAlgorithm;
    }

    /**
     * Set the xml encryption algorithm.
     * @param xencAlgorithm
     */
    public void setXencAlgorithm(String xencAlgorithm) {
        if (xencAlgorithm == null) {
            throw new IllegalArgumentException();
        }
        this.xencAlgorithm = xencAlgorithm;
    }

    private String xencAlgorithm = XencUtil.AES_128_CBC;

}
