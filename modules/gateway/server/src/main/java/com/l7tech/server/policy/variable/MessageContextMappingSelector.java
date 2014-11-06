package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.policy.variable.Syntax;

public class MessageContextMappingSelector implements ExpandVariables.Selector<MessageContextMapping> {

    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String TYPE = "type";

    @Override
    public Selection select(String contextName, MessageContextMapping context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        String attr = name.toLowerCase();

        if (attr.equals(KEY)) {
            return new Selection(context.getKey());
        } else if (attr.equals(VALUE)) {
            return new Selection( context.getValue() );
        } else if (attr.equals(TYPE)) {
            return new Selection( context.getMappingType() );
        } else {
            String msg = handler.handleBadVariable("Unable to process variable name: " + name);
            if (strict) throw new IllegalArgumentException(msg);
            return null;
        }
    }

    @Override
    public Class<MessageContextMapping> getContextObjectClass() {
        return MessageContextMapping.class;
    }
}
