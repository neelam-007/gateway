package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.util.SoapUtil;

import javax.xml.rpc.NamespaceConstants;
import javax.xml.soap.SOAPConstants;
import java.util.Map;
import java.util.HashMap;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
public class ResponseWssIntegrity extends XpathBasedAssertion {
    public ResponseWssIntegrity() {
        XpathExpression xpath = new XpathExpression();
        xpath.setExpression(SoapUtil.SOAP_BODY_XPATH);
        Map nss = new HashMap();
        nss.put(NamespaceConstants.NSPREFIX_SOAP_ENVELOPE, SOAPConstants.URI_NS_SOAP_ENVELOPE);
        xpath.setNamespaces(nss);
        setXpathExpression(xpath);
    }

    public ResponseWssIntegrity(XpathExpression xpath) {
        setXpathExpression(xpath);
    }
}
