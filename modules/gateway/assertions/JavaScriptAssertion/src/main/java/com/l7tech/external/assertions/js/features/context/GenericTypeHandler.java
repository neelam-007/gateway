package com.l7tech.external.assertions.js.features.context;

import com.l7tech.server.message.PolicyEnforcementContext;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.Undefined;

/**
 * Generic handler to write any generic object to the context.
 */
public class GenericTypeHandler implements ContextDataTypeHandler {

    @Override
    public void set(final PolicyEnforcementContext context, final ScriptObjectMirror jsonScriptObjectMirror,
                    final String name, final Object value) {
        if (value instanceof Undefined) {
            context.setVariable(name, null);
        } else {
            context.setVariable(name, value);
        }
    }
}
