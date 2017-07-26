package com.l7tech.external.assertions.circuitbreaker.console;

import com.l7tech.console.tree.policy.CompositeAssertionTreeNode;
import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@SuppressWarnings("unused")
public class CircuitBreakerAssertionTreeNode extends CompositeAssertionTreeNode<CircuitBreakerAssertion> {

    public CircuitBreakerAssertionTreeNode(CircuitBreakerAssertion assertion) {
        super(assertion);
    }

}
