package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.policy.assertion.XpathBasedAssertion;

import javax.xml.soap.SOAPConstants;
import javax.xml.rpc.NamespaceConstants;
import java.util.HashMap;
import java.util.Map;

/**
 * Enforces that a specific element in a request is signed.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
public class RequestWssIntegrity extends XpathBasedAssertion {
    public RequestWssIntegrity() {
        // default constructor that requests a signature on the body
        XpathExpression xpath = new XpathExpression();
        xpath.setExpression(SoapUtil.SOAP_BODY_XPATH);
        Map nss = new HashMap();
        nss.put(NamespaceConstants.NSPREFIX_SOAP_ENVELOPE, SOAPConstants.URI_NS_SOAP_ENVELOPE);
        xpath.setNamespaces(nss);
        setXpathExpression(xpath);
    }

    public RequestWssIntegrity(XpathExpression xpath) {
        setXpathExpression(xpath);
    }
}
