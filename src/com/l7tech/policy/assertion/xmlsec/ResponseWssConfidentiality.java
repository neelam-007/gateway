package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.XpathExpression;

import javax.xml.soap.SOAPConstants;
import javax.xml.rpc.NamespaceConstants;
import java.util.HashMap;
import java.util.Map;

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
        XpathExpression xpath = new XpathExpression();
        xpath.setExpression(SoapUtil.SOAP_BODY_XPATH);
        Map nss = new HashMap();
        nss.put(NamespaceConstants.NSPREFIX_SOAP_ENVELOPE, SOAPConstants.URI_NS_SOAP_ENVELOPE);
        xpath.setNamespaces(nss);
        setXpathExpression(xpath);
    }

    public ResponseWssConfidentiality(XpathExpression xpath) {
        setXpathExpression(xpath);
    }
}
