package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.variable.Syntax;

public class GoidSelector implements ExpandVariables.Selector<Goid> {

    private static final String ID = "hex";
    private static final String COMPRESSED = "compressed";

    @Override
    public Selection select(String contextName, Goid context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        String attr = name.toLowerCase();
        if (attr.equals(ID) || attr.isEmpty()) {
            return new Selection(context.toHexString());
        } else if (attr.equals(COMPRESSED)) {
            return new Selection( context.toCompressedString() );
        } else {
            String msg = handler.handleBadVariable("Unable to process variable name: " + name);
            if (strict) throw new IllegalArgumentException(msg);
            return null;
        }
    }

    @Override
    public Class<Goid> getContextObjectClass() {
        return Goid.class;
    }
}
