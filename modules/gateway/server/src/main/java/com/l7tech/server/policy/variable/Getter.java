/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * @author alex
 */
abstract class Getter {
    abstract Object get(String name, PolicyEnforcementContext context);

    /**
     * Check if this variable should exist as a built-in variable for the specified PolicyEnforcementContext.
     *
     * @param context the context that may later be passed to {@link #get} (if this method returns true).
     *                A value of null for the context means this method should return true only if the variable in question
     *                is available in all contexts.
     * @return true if this context should offer this variable as a built-in variable.  False if this variable does not apply
     *              to this type of context (ie, audit.* variables in a non-audit-sink policy enforcement context).
     */
    boolean isValidForContext(PolicyEnforcementContext context) {
        return true;
    }
}
