package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.xml.XpathExpression;

/**
 * Enforces that a specific element in a request is signed.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
public class RequestWssIntegrity extends XmlSecurityAssertionBase {
    public RequestWssIntegrity() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public RequestWssIntegrity(XpathExpression xpath) {
        setXpathExpression(xpath);
    }
}
