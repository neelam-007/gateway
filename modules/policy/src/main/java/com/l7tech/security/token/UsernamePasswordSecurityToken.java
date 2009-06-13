package com.l7tech.security.token;

import java.net.PasswordAuthentication;

/**
 * Generic token for username/password credential.
 */
public class UsernamePasswordSecurityToken implements SecurityToken, HasUsernameAndPassword {

    //- PUBLIC

    public UsernamePasswordSecurityToken( final SecurityTokenType securityTokenType,
                                          final PasswordAuthentication passwordAuth ) {
        this.securityTokenType = securityTokenType;
        this.passwordAuth = passwordAuth;
    }

    public UsernamePasswordSecurityToken( final SecurityTokenType securityTokenType,
                                          final String username,
                                          final char[] password ) {
        this.securityTokenType = securityTokenType;
        this.passwordAuth = new PasswordAuthentication( username, password );
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.HTTP_BASIC;
    }

    @Override
    public String getUsername() {
        return passwordAuth.getUserName();
    }

    @Override
    public char[] getPassword() {
        return passwordAuth.getPassword();
    }

    //- PRIVATE

    private final SecurityTokenType securityTokenType;
    private final PasswordAuthentication passwordAuth;

}
