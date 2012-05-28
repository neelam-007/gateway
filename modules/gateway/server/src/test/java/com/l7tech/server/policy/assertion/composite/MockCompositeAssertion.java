package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.util.TimeSource;

public class MockCompositeAssertion extends CompositeAssertion {
    
    private TimeSource timeSource;

    public TimeSource getTimeSource() {
        return timeSource;
    }

    public void setTimeSource(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, ServerMockCompositeAssertion.class.getName());
        return meta;
    }
}
