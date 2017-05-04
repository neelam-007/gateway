package com.l7tech.server.identity.ldap;

import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.ldap.BindOnlyLdapUser;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;

import javax.naming.AuthenticationException;

/**
 * User manager interface for bind-only provider.
 */
public interface BindOnlyLdapUserManager extends UserManager<BindOnlyLdapUser> {
    boolean authenticateBasic(String dn, String passwd) throws AuthenticationException;

    AuthenticationResult authenticatePasswordCredentials(LoginCredentials pc) throws BadCredentialsException, BadUsernamePatternException, AuthenticationException;

    String makeDn(String login) throws BadUsernamePatternException;

    void configure(BindOnlyLdapIdentityProvider provider);

    void setLdapRuntimeConfig(LdapRuntimeConfig ldapRuntimeConfig);

    public static class BadUsernamePatternException extends Exception {
        public BadUsernamePatternException() {
            super("Username contains characters disallowed by current Simple LDAP username pattern");
        }
    }
}
