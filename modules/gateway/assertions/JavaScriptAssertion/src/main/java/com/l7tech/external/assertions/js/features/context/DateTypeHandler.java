package com.l7tech.external.assertions.js.features.context;

import com.l7tech.server.message.PolicyEnforcementContext;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.Date;

/**
 * Handler to write Date object to the context.
 */
public class DateTypeHandler implements ContextDataTypeHandler {

    @Override
    public void set(final PolicyEnforcementContext context, final ScriptObjectMirror jsonScriptObjectMirror,
                    final String name, final Object value) {
        long epochTimeInMilliseconds;
        if (value instanceof ScriptObjectMirror) {
            final ScriptObjectMirror scriptObjectMirror = (ScriptObjectMirror) value;
            epochTimeInMilliseconds = Math.round((Double) scriptObjectMirror.callMember("getTime"));
        } else {
            epochTimeInMilliseconds = ((Date) value).getTime();
        }
        context.setVariable(name, new Date(epochTimeInMilliseconds));
    }
}
