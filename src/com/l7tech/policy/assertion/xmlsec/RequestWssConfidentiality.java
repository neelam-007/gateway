package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.security.xml.XencUtil;

/**
 * Enforces the XML security on the message elements or entire message
 * 
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
public class RequestWssConfidentiality extends XmlSecurityAssertionBase {
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
    public String getxEncAlgorithm() {
        return xEncAlgorithm;
    }

    /**
     * Set the xml encryption algorithm.
     * @param xEncAlgorithm
     */
    public void setxEncAlgorithm(String xEncAlgorithm) {
        if (xEncAlgorithm == null) {
            throw new IllegalArgumentException();
        }
        this.xEncAlgorithm = xEncAlgorithm;
    }

    private String xEncAlgorithm = XencUtil.AES_128_CBC;
}
