package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.util.ISO8601Date;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * @author ghuang
 */
public class SecureConversationSessionSelector implements ExpandVariables.Selector<SecureConversationSession> {
    private static final Pattern PATTERN_PERIOD = Pattern.compile("\\.");

    @Override
    public Selection select(String contextName, SecureConversationSession context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if (context == null) return null;

        if ("id".equalsIgnoreCase(name)) {
            return new Selection(context.getIdentifier());
        } else if (name != null && name.startsWith("user")) {
            String[] names = PATTERN_PERIOD.split(name, 2);
            String remainingName = names.length > 1 ? names[1] : null;
            if (remainingName == null) {
                return new Selection(context.getUsedBy());
            } else {
                return new Selection(context.getUsedBy(), remainingName);
            }
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