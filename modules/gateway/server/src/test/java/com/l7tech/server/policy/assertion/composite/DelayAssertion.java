package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetVariableAssertion;

/**
 * Created by IntelliJ IDEA.
 * User: awitrisna
 * Date: 23/04/12
 * Time: 2:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class DelayAssertion extends SetVariableAssertion {

    public DelayAssertion() {
    }

    public DelayAssertion(String variableToSet, String stringValue, long delay) {
        super(variableToSet, stringValue);
        this.delay = delay;
    }

    private long delay;

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, ServerDelayAssertion.class.getName());
        return meta;
    }

}
