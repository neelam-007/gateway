package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.xml.XpathExpression;

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
}
