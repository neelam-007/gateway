package com.l7tech.external.assertions.js.features.context;

import com.l7tech.server.message.PolicyEnforcementContext;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * Handler to write integers and floating point numbers to the context.
 */
public class NumberTypeHandler implements ContextDataTypeHandler {

    private static final double LONG_MAX_DOUBLE_VALUE = Long.MAX_VALUE;
    private static final double LONG_MIN_DOUBLE_VALUE = Long.MIN_VALUE;

    @Override
    public void set(final PolicyEnforcementContext context, final ScriptObjectMirror jsonScriptObjectMirror,
                    final String name, final Object value) {
        if (value instanceof Double) {
            // If Double, try parsing to Long as it may still be a integer value with 64-bit.
            final Double variable = (Double) value;
            if (checkIfDoubleIsIntegerOrLongType(variable)) {
                Long longValue = variable.longValue();
                if (longValue > Integer.MAX_VALUE) {
                    context.setVariable(name, longValue);
                } else {
                    context.setVariable(name, longValue.intValue());
                }
                return;
            }
        }
        context.setVariable(name, value);
    }

    private boolean checkIfDoubleIsIntegerOrLongType(final Double value) {
        return (value == Math.floor(value)) && !Double.isInfinite(value) && value <= LONG_MAX_DOUBLE_VALUE && value >= LONG_MIN_DOUBLE_VALUE;
    }
}
