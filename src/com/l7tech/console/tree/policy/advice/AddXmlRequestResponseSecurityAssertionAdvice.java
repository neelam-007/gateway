package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.security.xml.ElementSecurity;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertion;

import javax.xml.soap.SOAPConstants;
import java.util.HashMap;
import java.util.Map;

/**
 * The class <code>AddXmlRequestResponseSecurityAssertionAdvice</code>
 * intercepts policy XML reguest/response security add. It sets security
 * defaults such as default envelope signing.
 * <p/>
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class AddXmlRequestResponseSecurityAssertionAdvice implements Advice {
    private Map namespaces = new HashMap();


    public AddXmlRequestResponseSecurityAssertionAdvice() {
        namespaces.put("soapenv", SOAPConstants.URI_NS_SOAP_ENVELOPE);
        namespaces.put("SOAP-ENV", SOAPConstants.URI_NS_SOAP_ENVELOPE);
    }

    /**
     * Intercepts a policy change.
     * 
     * @param pc The policy change.
     */
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 ||
          !(assertions[0] instanceof XmlSecurityAssertion)) {
            throw new IllegalArgumentException();
        }
        XpathExpression xpathExpression = new XpathExpression(SoapUtil.SOAP_ENVELOPE_XPATH, namespaces);
        XmlSecurityAssertion a = (XmlSecurityAssertion)assertions[0];
        final ElementSecurity elementSecurity =
          new ElementSecurity(xpathExpression, null, false, ElementSecurity.DEFAULT_CIPHER, ElementSecurity.DEFAULT_KEYBITS);
        a.setElements(new ElementSecurity[]{elementSecurity});
        pc.proceed();
    }
}
