/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * A built-in variable (accessible from {@link ServerVariables} and the strategy
 * for getting it from a {@link PolicyEnforcementContext}.
 */
class Variable {
    private final Getter getter;
    private final String name;

    Variable(String name, Getter getter) {
        this.name = name;
        this.getter = getter;
    }

    public String getName() {
        return name;
    }

    Object get(String name, PolicyEnforcementContext context) throws NoSuchVariableException {
        if (getter == null)
            return context.getVariable(name);
        else
            return getter.get(name, context);
    }

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
        return getter == null || getter.isValidForContext(context);
    }
}
