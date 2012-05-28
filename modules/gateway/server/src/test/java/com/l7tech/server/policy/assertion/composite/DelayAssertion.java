package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.util.TimeSource;

public class DelayAssertion extends SetVariableAssertion {

    public DelayAssertion() {
    }

    public DelayAssertion(String variableToSet, String stringValue, long delay, TimeSource timeSource) {
        super(variableToSet, stringValue);
        this.delay = delay;
        this.timeSource = timeSource;
    }

    private long delay;
    private TimeSource timeSource;

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public TimeSource getTimeSource() {
        return timeSource;
    }

    public void setTimeSource(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, ServerDelayAssertion.class.getName());
        return meta;
    }

}
