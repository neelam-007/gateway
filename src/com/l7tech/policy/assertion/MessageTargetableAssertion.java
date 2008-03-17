/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

/** @author alex */
public abstract class MessageTargetableAssertion extends Assertion implements MessageTargetable, UsesVariables {
    private TargetMessageType target = TargetMessageType.REQUEST;
    private String otherTargetMessageVariable;

    public TargetMessageType getTarget() {
        return target;
    }

    public void setTarget(TargetMessageType target) {
        if (target == null) throw new NullPointerException();
        this.target = target;
    }

    public String getOtherTargetMessageVariable() {
        return otherTargetMessageVariable;
    }

    public void setOtherTargetMessageVariable(String otherTargetMessageVariable) {
        this.otherTargetMessageVariable = otherTargetMessageVariable;
    }

    public String getTargetName() {
        switch(target) {
            case REQUEST:
                return "request";
            case RESPONSE:
                return "response";
            case OTHER:
                return "${" + otherTargetMessageVariable + "}";
            default:
                throw new IllegalStateException();
        }
    }

    public String[] getVariablesUsed() {
        if (otherTargetMessageVariable != null) return new String[] { otherTargetMessageVariable };
        return new String[0];
    }
}
