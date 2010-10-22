package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.xml.soap.SoapVersion;

/**
 * When an XPathBasedAssertion is added to a policy, check if its XpathExpression needs to be
 * initialized with a default XPath expression.
 */
public class AddXPathAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof XpathBasedAssertion)) {
            throw new IllegalArgumentException();
        }
        XpathBasedAssertion subject = (XpathBasedAssertion)assertions[0];

        if (subject.getXpathExpression() == null) {
            // Assertion has not yet assigned a default XPath expression; initialize it now
            Policy policy = pc.getPolicyFragment();
            PublishedService service = pc.getService();
            if (policy == null && service != null)
                policy = service.getPolicy();

            final boolean soap = policy != null && policy.isSoap();
            final SoapVersion soapVersion = service == null ? null : service.getSoapVersion();
            subject.setXpathExpression(subject.createDefaultXpathExpression(soap, soapVersion));
        }

        pc.proceed();
    }
}
