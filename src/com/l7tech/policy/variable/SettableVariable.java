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

    SettableVariable(String name, Getter getter, Setter setter, boolean prefixed, boolean multivalued, String resourceKey) {
        super(name, getter, prefixed, multivalued, true, resourceKey);
        this.setter = setter;
    }

    SettableVariable(String name, Getter getter, Setter setter) {
        this(name, getter, setter, false, false, null);
    }

    void set(String name, Object value, PolicyEnforcementContext context) {
        if (setter != null) {
            setter.set(name, value, context);
        } else {
            context.setVariable(name, value);
        }
    }
}
