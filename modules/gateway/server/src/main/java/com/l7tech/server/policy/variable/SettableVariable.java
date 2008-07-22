/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * @author alex
 */
class SettableVariable extends Variable {
    private final Setter setter;

    SettableVariable(String name, Getter getter, Setter setter) {
        super(name, getter);
        this.setter = setter;
    }

    void set(String name, Object value, PolicyEnforcementContext context) {
        if (setter != null) {
            setter.set(name, value, context);
        } else {
            context.setVariable(name, value);
        }
    }
}
