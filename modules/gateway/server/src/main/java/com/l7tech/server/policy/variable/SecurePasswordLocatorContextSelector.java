package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.Syntax;

import java.util.regex.Pattern;

/**
 * A Selector that locates a SecurePassword instance by taking the next name component to be a SecurePassword's name.
 * <p/>
 * This will extract the next name component, look up the appropriate SecurePassword instance,
 * check to ensure that it enables use via context variable interpolation, and then return the SecurePassword for further selection.
 */
public class SecurePasswordLocatorContextSelector implements ExpandVariables.Selector<SecurePasswordLocatorContext> {
    private static final Pattern PATTERN_PERIOD = Pattern.compile("\\.");

    @Override
    public Selection select(String contextName, SecurePasswordLocatorContext context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        String[] names = PATTERN_PERIOD.split(name, 2);
        if (names == null || names.length < 1) {
            handler.handleBadVariable("Missing secure password name in " + contextName);
            return null;
        }

        String secpassName = names[0];
        String remainingName = names.length > 1 ? names[1] : null;

        final SecurePassword securePassword;
        try {
            securePassword = ServerVariables.findSecurePasswordByName(secpassName);
        } catch (FindException e) {
            handler.handleBadVariable("Failed to look up stored password named " + secpassName, e);
            return null;
        }

        if (securePassword == null) {
            handler.handleBadVariable("No stored password named " + secpassName);
            return null;
        }

        if (!securePassword.isUsageFromVariable()) {
            handler.handleBadVariable("Use via context variable not allowed for secure password name " + secpassName);
            return null;
        }

        return new Selection(securePassword, remainingName);
    }

    @Override
    public Class<SecurePasswordLocatorContext> getContextObjectClass() {
        return SecurePasswordLocatorContext.class;
    }
}
