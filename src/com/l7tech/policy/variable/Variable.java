/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * A built-in variable (accessible from {@link BuiltinVariables} and the strategy
 * for getting it from a {@link PolicyEnforcementContext}.
 */
class Variable {
    private final Getter getter;
    private final VariableMetadata metadata;

    Variable(String name, Getter getter) {
        this(name, getter, null);
    }

    Variable(String name, Getter getter, String resourceKey) {
        this(name, getter, false, false, false, resourceKey);
    }

    Variable(String name, Getter getter, boolean prefixed, boolean multivalued) {
        this(name, getter, prefixed, multivalued, false, null);
    }

    Variable(String name, Getter getter, boolean prefixed, boolean multivalued, boolean settable, String resourceKey) {
        this.metadata = new VariableMetadata(name, prefixed, multivalued, resourceKey, false);
        this.getter = getter;
    }

    VariableMetadata getMetadata() {
        return metadata;
    }

    Object get(String name, PolicyEnforcementContext context) throws NoSuchVariableException {
        if (getter == null)
            return context.getVariable(name);
        else
            return getter.get(name, context);
    }

}
