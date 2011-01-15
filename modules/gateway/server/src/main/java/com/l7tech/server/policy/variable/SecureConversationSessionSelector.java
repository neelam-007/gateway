package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.util.ISO8601Date;

import java.util.Date;

/**
 * @author ghuang
 */
public class SecureConversationSessionSelector implements ExpandVariables.Selector<SecureConversationSession> {
    @Override
    public Selection select(String contextName, SecureConversationSession context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if (context == null) return null;

        if ("id".equalsIgnoreCase(name)) {
            return new Selection(context.getIdentifier());
        } else if ("user".equalsIgnoreCase(name)) {
            return new Selection(context.getUsedBy());
        } else if ("creation".equalsIgnoreCase(name)) {
            return new Selection(ISO8601Date.format(new Date(context.getCreation())));
        } else if ("expiration".equalsIgnoreCase(name)) {
            return new Selection(ISO8601Date.format(new Date(context.getExpiration())));
        } else if ("scNamespace".equalsIgnoreCase(name)) {
            return new Selection(context.getSCNamespace());
        }

        return null;
    }

    @Override
    public Class<SecureConversationSession> getContextObjectClass() {
        return SecureConversationSession.class;
    }
}