package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.XpathBasedAssertion;

import javax.xml.rpc.NamespaceConstants;
import javax.xml.soap.SOAPConstants;
import java.util.HashMap;
import java.util.Map;

/**
 * Enforces the XML security on the message elements or entire message
 * 
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
public class RequestWssConfidentiality extends XpathBasedAssertion {
    public RequestWssConfidentiality() {
        // default constructor that requests a signature on the body
        XpathExpression xpath = new XpathExpression();
        xpath.setExpression(SoapUtil.SOAP_BODY_XPATH);
        Map nss = new HashMap();
        nss.put(NamespaceConstants.NSPREFIX_SOAP_ENVELOPE, SOAPConstants.URI_NS_SOAP_ENVELOPE);
        xpath.setNamespaces(nss);
        setXpathExpression(xpath);
    }

    public RequestWssConfidentiality(XpathExpression xpath) {
        setXpathExpression(xpath);
    }
}
