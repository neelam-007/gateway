package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.util.TestTimeSource;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class MockAssertion extends Assertion {

    private AssertionStatus returnStatus;
    private long executionTime;
    private TestTimeSource timeSource;

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

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public TestTimeSource getTimeSource() {
        return timeSource;
    }

    public void setTimeSource(TestTimeSource timeSource) {
        this.timeSource = timeSource;
    }
}
