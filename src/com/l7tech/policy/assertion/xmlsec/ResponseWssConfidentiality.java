package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.XpathBasedAssertion;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
public class ResponseWssConfidentiality extends XpathBasedAssertion {
    public ResponseWssConfidentiality() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public ResponseWssConfidentiality(XpathExpression xpath) {
        setXpathExpression(xpath);
    }
}
