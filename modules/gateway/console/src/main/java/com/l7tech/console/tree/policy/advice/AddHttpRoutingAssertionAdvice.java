package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;

/**
 * Advice called whenever a new HttpRoutingAssertion is added to policy.
 */
public class AddHttpRoutingAssertionAdvice implements Advice {
    @Override
    public void proceed(final PolicyChange pc) {
        if (pc != null) {
            final Assertion[] assertions = pc.getEvent().getChildren();
            if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof HttpRoutingAssertion)) {
                throw new IllegalArgumentException("Expected one HttpRoutingAssertion but received: " + assertions);
            }
            final HttpRoutingAssertion routeAssertion = (HttpRoutingAssertion) assertions[0];
            // SSG-7778 change new instances of HttpRouteAssertion added into policy to pass all headers by default
            // while maintaining backwards compatibility
            routeAssertion.getRequestHeaderRules().setForwardAll(true);
            routeAssertion.getResponseHeaderRules().setForwardAll(true);
            pc.proceed();
        }
    }
}
