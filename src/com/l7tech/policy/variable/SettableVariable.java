/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * @author alex
 */
class SettableVariable extends Variable {
    private final Setter setter;

    public SettableVariable(String name, Getter getter, Setter setter, boolean prefixed, boolean multivalued) {
        super(name, getter, prefixed, multivalued);
        this.setter = setter;
    }

    public SettableVariable(String name, Getter getter, Setter setter) {
        this(name, getter, setter, false, false);
    }


    public void set(String name, Object value, PolicyEnforcementContext context) {
        if (setter != null) {
            setter.set(name, value, context);
        } else {
            context.setVariable(name, value);
        }
    }
}
