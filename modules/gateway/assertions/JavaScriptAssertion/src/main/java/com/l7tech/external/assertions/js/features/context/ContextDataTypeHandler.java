package com.l7tech.external.assertions.js.features.context;

import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * Interface for all handlers writing data to the context.
 */
public interface ContextDataTypeHandler {

    /**
     * Sets the variable to the context.
     * @param context
     * @param jsonScriptObjectMirror
     * @param name
     * @param value
     * @throws VariableNotSettableException
     */
    void set(final PolicyEnforcementContext context, final ScriptObjectMirror jsonScriptObjectMirror,
             final String name, final Object value) throws VariableNotSettableException;
}
