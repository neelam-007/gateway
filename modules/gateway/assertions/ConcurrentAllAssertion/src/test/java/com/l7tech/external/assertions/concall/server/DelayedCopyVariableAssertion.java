package com.l7tech.external.assertions.concall.server;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;

/**
 *
 */
public class DelayedCopyVariableAssertion extends Assertion implements UsesVariables, SetsVariables {
    long delayMillis = 1000L;
    String sourceVariableName = "sourceVar";
    String destVariableName = "destVar";

    public DelayedCopyVariableAssertion() {
    }

    public DelayedCopyVariableAssertion(long delayMillis, String sourceVariableName, String destVariableName) {
        this.delayMillis = delayMillis;
        this.sourceVariableName = sourceVariableName;
        this.destVariableName = destVariableName;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    public void setDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public String getSourceVariableName() {
        return sourceVariableName;
    }

    public void setSourceVariableName(String sourceVariableName) {
        this.sourceVariableName = sourceVariableName;
    }

    public String getDestVariableName() {
        return destVariableName;
    }

    public void setDestVariableName(String destVariableName) {
        this.destVariableName = destVariableName;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, ServerDelayedCopyVariableAssertion.class.getName());
        return meta;
    }

    @Override
    public String[] getVariablesUsed() {
        return new String[] { sourceVariableName };
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] { new VariableMetadata(destVariableName, false, false, destVariableName, true) };
    }
}
