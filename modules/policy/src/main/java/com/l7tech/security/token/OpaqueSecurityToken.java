package com.l7tech.security.token;

/**
 *
 */
public class OpaqueSecurityToken implements SecurityToken {

    //- PUBLIC

    public OpaqueSecurityToken() {
        this.username = "<unknown>";
        this.credential = null;
    }

    public OpaqueSecurityToken( final String username,
                                final char[] credential ) {
        this.username = username;
        this.credential = credential;
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.UNKNOWN;
    }

    public char[] getCredential() {
        return credential;
    }

    public String getUsername() {
        return username;
    }

    //- PRIVATE

    private final String username;
    private final char[] credential;
}
