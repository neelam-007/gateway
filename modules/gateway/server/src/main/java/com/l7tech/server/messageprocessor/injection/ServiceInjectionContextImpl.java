package com.l7tech.server.messageprocessor.injection;

import com.ca.apim.gateway.extension.processorinjection.ServiceInjectionContext;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.jetbrains.annotations.NotNull;

public class ServiceInjectionContextImpl implements ServiceInjectionContext {
    private final PolicyEnforcementContext context;

    ServiceInjectionContextImpl(@NotNull final PolicyEnforcementContext context) {
        this.context = context;
    }

    @Override
    public Object getVariable(String name) {
        try {
            return context.getVariable(name);
        } catch (NoSuchVariableException e) {
            return null;
        }
    }

    @Override
    public void setVariable(String name, Object value) {
        context.setVariable(name, value);
    }
}
