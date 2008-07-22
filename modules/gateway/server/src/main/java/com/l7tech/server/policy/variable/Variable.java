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

}
