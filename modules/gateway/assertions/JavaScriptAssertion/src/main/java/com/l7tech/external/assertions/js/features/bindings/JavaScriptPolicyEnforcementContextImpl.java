package com.l7tech.external.assertions.js.features.bindings;

import com.l7tech.external.assertions.js.features.context.ContextDataTypeHandlerRegistry;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.server.message.PolicyEnforcementContext;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Context implementation to expose the PolicyEnforcementContext to Javascript.
 */
public class JavaScriptPolicyEnforcementContextImpl implements JavaScriptPolicyEnforcementContext {

    private PolicyEnforcementContext policyContext;
    private ScriptObjectMirror jsonScriptObjectMirror;

    public JavaScriptPolicyEnforcementContextImpl(final PolicyEnforcementContext ctx,
                                                  final ScriptObjectMirror jsonScriptObjectMirror) {
        this.policyContext = ctx;
        this.jsonScriptObjectMirror = jsonScriptObjectMirror;
    }

    @Override
    public Object getVariable(@NotNull final String name) throws NoSuchVariableException {
        if (StringUtils.isNotBlank(name)) {
            final String[] variableViews = name.trim().split(":");
            if (variableViews.length > 2) {
                throw new NoSuchVariableException(name, "Invalid format of the variable view. Valid format: <variable>:<view>");
            }

            final Object obj = policyContext.getVariable(variableViews[0]);
            if (obj instanceof Message) {
                final Message message = (Message) obj;
                if (variableViews.length > 1) {
                    return JavaScriptMessageFactory.getInstance().get(message, variableViews[1], jsonScriptObjectMirror);
                }
                return JavaScriptMessageFactory.getInstance().get(message, jsonScriptObjectMirror);
            }
            return obj;
        }
        return null;
    }

    @Override
    public void setVariable(@NotNull final String name, final Object scriptObj) {
        if (StringUtils.isNotBlank(name)) {
            VariableMetadata.assertNameIsValid(StringUtils.trim(name), false);
            ContextDataTypeHandlerRegistry.getHandler(scriptObj).set(policyContext, jsonScriptObjectMirror, StringUtils.trim(name), scriptObj);
        }
    }
}