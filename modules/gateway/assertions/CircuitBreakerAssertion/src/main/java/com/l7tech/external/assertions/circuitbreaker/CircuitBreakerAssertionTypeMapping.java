package com.l7tech.external.assertions.circuitbreaker;

import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.*;
import org.w3c.dom.Element;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@SuppressWarnings("unused")
public class CircuitBreakerAssertionTypeMapping extends CompositeAssertionMapping {
    public CircuitBreakerAssertionTypeMapping() {
        super(new CircuitBreakerAssertion(), "CircuitBreaker");
    }
    
    @Override
    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) throws InvalidPolicyTreeException {
        super.populateElement(wspWriter, element, object);
    }

    @Override
    protected void populateObject(CompositeAssertion cass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        super.populateObject(cass, source, visitor);
    }

    @Override
    protected String getNsPrefix() {
        return "L7p:";
    }

    @Override
    protected String getNsUri() {
        return WspConstants.L7_POLICY_NS;
    }
}