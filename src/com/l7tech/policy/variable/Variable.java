/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * A built-in variable (accessible from {@link BuiltinVariables} and the strategy for getting it
 * from a {@link PolicyEnforcementContext}.
 */
class Variable {
    private final String name;
    private final Getter getter;
    private final boolean prefixed;
    private final boolean multivalued;

    Variable(String name, Getter getter) {
        this(name, getter, false, false);
    }

    public Variable(String name, Getter getter, boolean prefixed, boolean multivalued) {
        this.name = name;
        this.getter = getter;
        this.prefixed = prefixed;
        this.multivalued = multivalued;
    }

    String getName() {
        return name;
    }

    public boolean isPrefixed() {
        return prefixed;
    }

    public boolean isMultivalued() {
        return multivalued;
    }

    public Object get(String name, PolicyEnforcementContext context) throws NoSuchVariableException {
        if (getter == null)
            return context.getVariable(name);
        else
            return getter.get(name, context);
    }

}
