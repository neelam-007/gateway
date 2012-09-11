package com.l7tech.server.policy.variable;

import com.l7tech.common.io.failover.Service;
import com.l7tech.policy.variable.Syntax;

import java.util.concurrent.TimeUnit;

/**
 * Selector for the service properties
 */
public class ServiceSelector implements ExpandVariables.Selector<Service> {

    private static final String NAME = "name";
    private static final String PROPERTIES = "properties";

    @Override
    public Selection select(String contextName, Service context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        String attr = name.toLowerCase();
        if (attr.equals(NAME)) {
            return new Selection(context.getName());
        } else if (attr.equals(PROPERTIES)) {
            return new Selection(context.getProperties());
        } else {
            String msg = handler.handleBadVariable("Unable to process variable name: " + name);
            if (strict) throw new IllegalArgumentException(msg);
            return null;
        }
    }

    @Override
    public Class<Service> getContextObjectClass() {
        return Service.class;
    }
}
