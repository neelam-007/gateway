package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class MockAssertion extends Assertion {

    private AssertionStatus returnStatus;

    public MockAssertion() {

    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, ServerMockAssertion.class.getName());
        return meta;
    }

    public AssertionStatus getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(AssertionStatus returnStatus) {
        this.returnStatus = returnStatus;
    }
}
