package com.l7tech.server.policy.variable;

import com.l7tech.server.trace.TracePolicyEnforcementContext;

/**
 * Object to use as target for selection of trace.* variables.  This is a thin wrapper around the trace PEC just
 * so that we don't end up registering a selector for a subclass of PolicyEnforcementContext (to avoid collisions
 * with the main PolicyEnforcementContextSelector).
 */
public class DebugTraceVariableContext {
    final TracePolicyEnforcementContext context;

    public DebugTraceVariableContext(TracePolicyEnforcementContext context) {
        if (context == null) throw new NullPointerException();
        this.context = context;
    }

    /**
     * @return the trace PEC.  Never null.
     */
    public TracePolicyEnforcementContext getContext() {
        return context;
    }
}
