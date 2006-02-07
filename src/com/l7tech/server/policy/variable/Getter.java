/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * @author alex
 */
interface Getter {
    Object get(String name, PolicyEnforcementContext context);
}
