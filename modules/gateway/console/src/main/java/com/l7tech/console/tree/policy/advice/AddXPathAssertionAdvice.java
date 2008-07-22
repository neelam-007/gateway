package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SimpleXpathAssertion;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.util.SoapConstants;

import java.util.HashMap;

/**
 * Things that happen when you drop an xpath assertion into a policy.
 * For non-soap services, the default path is "/" instead of soap envelope
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 28, 2007<br/>
 */
public class AddXPathAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SimpleXpathAssertion)) {
            throw new IllegalArgumentException();
        }
        SimpleXpathAssertion subject = (SimpleXpathAssertion)assertions[0];
        if (pc.getService() != null && !pc.getService().isSoap()) {
            // if this xpath has not been previously customized, then adjust default value
            // see bzilla #3870
            if ( SoapConstants.SOAP_ENVELOPE_XPATH.equals(subject.getXpathExpression().getExpression())) {
                subject.setXpathExpression(new XpathExpression("/", new HashMap()));
            }
        }
        pc.proceed();
    }
}
