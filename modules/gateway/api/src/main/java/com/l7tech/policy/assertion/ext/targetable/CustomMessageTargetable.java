/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.ext.targetable;

import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesVariables;

/**
 * Implemented by Custom Assertions that can be configured to target requests, responses or other message-typed variables.
 */
public interface CustomMessageTargetable extends SetsVariables, UsesVariables {

    /**
     * The name of message-typed variable to use as this assertion's target.
     */
    String getTargetMessageVariable();

    /**
     * Set the name of a message-typed variable to use as this assertion's target.
     */
    void setTargetMessageVariable(String otherMessageVariable);

    /**
     * A short, descriptive name for the target, i.e. "request", "response" or other message-typed variable
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
