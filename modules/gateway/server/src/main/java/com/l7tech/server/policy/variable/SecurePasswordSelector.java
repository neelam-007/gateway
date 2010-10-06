package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ExceptionUtils;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 *
 */
public class SecurePasswordSelector implements ExpandVariables.Selector<SecurePassword> {
    private static Logger logger = Logger.getLogger(SecurePasswordSelector.class.getName());

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Override
    public Selection select(String contextName, SecurePassword securePassword, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        name = name.toLowerCase();

        if (name.equals("name"))
            return new Selection(securePassword.getName());

        if (name.equals("description"))
            return new Selection(securePassword.getDescription());

        if (name.equals("plaintext")) {
            try {
                char[] pass = ServerVariables.getPlaintextPassword(securePassword);
                return new Selection(pass == null ? null : new String(pass));
            } catch (FindException e) {
                handler.handleBadVariable("Unable to look up secure password: " + ExceptionUtils.getMessage(e), e);
                return null;
            } catch (ParseException e) {
                handler.handleBadVariable("Unable to decode secure password: " + ExceptionUtils.getMessage(e), e);
                return null;
            }
        }

        handler.handleBadVariable(name + " in " + contextName);
        return null;
    }

    @Override
    public Class<SecurePassword> getContextObjectClass() {
        return SecurePassword.class;
    }
}
