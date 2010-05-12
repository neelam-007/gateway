package com.l7tech.server.security.kerberos;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.xml.processor.SecurityContext;

/**
 *
 */
public class KerberosSession implements SecurityContext {

    public KerberosSession( final String identifier,
                            final byte[] sharedSecret,
                            final long creation,
                            final long expiration,
                            final LoginCredentials credentials ) {
        this.identifier = identifier;
        this.sharedSecret = sharedSecret;
        this.creation = creation;
        this.expiration = expiration;
        this.credentials = credentials;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public byte[] getSharedSecret() {
        return sharedSecret;
    }

    @Override
    public SecurityToken getSecurityToken() {
        return credentials!=null ? credentials.getSecurityToken() : null;
    }

    public long getExpiration() {
        return expiration;
    }

    public long getCreation() {
        return creation;
    }

    public LoginCredentials getCredentials() {
        return credentials;
    }

    private final String identifier;
    private final byte[] sharedSecret;
    private final long expiration;
    private final long creation;
    private final LoginCredentials credentials;
}
