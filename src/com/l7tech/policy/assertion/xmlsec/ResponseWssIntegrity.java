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
public class ResponseWssIntegrity extends XmlSecurityAssertionBase {
    public ResponseWssIntegrity() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public ResponseWssIntegrity(XpathExpression xpath) {
        setXpathExpression(xpath);
    }
}
