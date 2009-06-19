package com.l7tech.server.policy.variable;

import com.l7tech.util.Functions;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.identity.User;
import com.l7tech.identity.ldap.LdapIdentity;
import com.l7tech.message.Message;

import java.util.List;
import java.util.logging.Logger;

/**
 * 
 */
class AuthenticatedUserGetter implements Getter {

    //- PUBLIC

    @Override
    public Object get( final String name, final PolicyEnforcementContext context ) {
        final List<AuthenticationResult> authResults = message==null ?
                context.getDefaultAuthenticationContext().getAllAuthenticationResults() :
                context.getAuthenticationContext(message).getAllAuthenticationResults();
        if (multivalued) {
            final List<String> strings = Functions.map(authResults, userToValue);
            return strings.toArray(new String[strings.size()]);
        }

        final int prefixLength = prefix.length();
        String suffix = name.substring(prefixLength);
        if (suffix.length() == 0) {
            // Without suffix
            return userToValue.call(context.getDefaultAuthenticationContext().getLastAuthenticationResult());
        }

        if (!suffix.startsWith("."))
            throw new IllegalArgumentException("Variable '" + name + "' does not have a period before the parameter name.");

        String indexS = name.substring(prefixLength + 1);
        try {
            int index = Integer.parseInt(indexS);
            if (index < 0 || index >= authResults.size()) {
                logger.info((index < 0 ? "authentication result index out of range: " : "not enough authentication results: ") + index);
                return null;
            }
            AuthenticationResult ar = authResults.get(index);
            return userToValue.call(ar);
        } catch (NumberFormatException e) {
            logger.info("Was expecting a number suffix with " + name + ". " + e.getMessage());
            return null;
        }
    }

    //- PACKAGE

   static final Functions.Unary<String,AuthenticationResult> USER_TO_NAME = new Functions.Unary<String, AuthenticationResult>() {
        @Override
        public String call(final AuthenticationResult authResult) {
            if (authResult == null) return null;
            User authenticatedUser = authResult.getUser();
            String user = null;
            if (authenticatedUser != null) {
                user = authenticatedUser.getName();
                if (user == null) user = authenticatedUser.getId();
            }
            return user;
        }
    };

    static final Functions.Unary<String,AuthenticationResult> USER_TO_DN = new Functions.Unary<String, AuthenticationResult>() {
        @Override
        public String call(final AuthenticationResult authResult) {
            if (authResult == null) return null;
            User user = authResult.getUser();
            if (user instanceof LdapIdentity ) {
                LdapIdentity ldapIdentity = (LdapIdentity) user;
                return ldapIdentity.getDn();
            }
            return user == null ? null : user.getSubjectDn();
        }
    };

    AuthenticatedUserGetter( final String prefix,
                             final boolean multivalued,
                             final Functions.Unary<String,AuthenticationResult> property ) {
        this(prefix, multivalued, property, null);
    }

    AuthenticatedUserGetter( final String prefix,
                             final boolean multivalued,
                             final Functions.Unary<String,AuthenticationResult> property,
                             final Message message ) {
        this.prefix = prefix;
        this.multivalued = multivalued;
        this.message = message;
        this.userToValue = property;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AuthenticatedUserGetter.class.getName());

    private final String prefix;
    private final boolean multivalued;
    private final Message message;
    private final Functions.Unary<String, AuthenticationResult> userToValue;

}
