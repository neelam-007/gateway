package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.XpathBasedAssertion;

/**
 * Enforces the XML security on the message elements or entire message
 * 
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
public class RequestWssConfidentiality extends XpathBasedAssertion {
    public RequestWssConfidentiality() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public RequestWssConfidentiality(XpathExpression xpath) {
        setXpathExpression(xpath);
    }
}
