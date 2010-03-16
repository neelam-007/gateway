/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

/**
 * Implemented by {@link Assertion}s that can be configured to target requests, responses or other message-typed variables.
 * @see MessageTargetableAssertion  
 * @author alex
 */
public interface MessageTargetable extends UsesVariables, SetsVariables {
    /**
     * The type of message this assertion targets.  Defaults to {@link TargetMessageType#REQUEST}. Never null.
     */
    TargetMessageType getTarget();

    /**
     * The type of message this assertion targets.  Defaults to {@link TargetMessageType#REQUEST}. Never null.
     */
    void setTarget(TargetMessageType target);

    /**
     * If {@link #getTarget} is {@link TargetMessageType#OTHER}, the name of some other message-typed variable to use as
     * this assertion's target.
     */
    String getOtherTargetMessageVariable();

    /**
     * If {@link #getTarget} is {@link TargetMessageType#OTHER}, the name of some other message-typed variable to use as
     * this assertion's target.
     */
    void setOtherTargetMessageVariable(String otherMessageVariable);

    /**
     * A short, descriptive name for the target, i.e. "request", "response" or {@link #getOtherTargetMessageVariable()}
     *
     * <p>Almost all MessageTargetable implementations will never return null,
     * in a few null is necessary for backwards compatibility.</p>
     *
     * @return the target name or null if no target is set.
     */
    String getTargetName();

    /**
     * @return true if the target message might be modified; false if the assertion only reads the target message.
     */
    boolean isTargetModifiedByGateway();
}
