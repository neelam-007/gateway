package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SimpleXpathAssertion;
import com.l7tech.common.xml.XpathExpression;
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
            subject.setXpathExpression(new XpathExpression("/", new HashMap()));
        }
        pc.proceed();
    }
}
