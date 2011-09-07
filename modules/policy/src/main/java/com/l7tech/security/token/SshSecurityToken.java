package com.l7tech.security.token;

import com.l7tech.message.SshKnob;

/**
 * SSH security token using user name, password and public key credential.
 */
public class SshSecurityToken implements SecurityToken, HasUsername {

    //- PUBLIC

    public SshSecurityToken(final SecurityTokenType securityTokenType,
                            final SshKnob.PublicKeyAuthentication publicKeyAuth) {
        this.securityTokenType = securityTokenType;
        this.publicKeyAuth = publicKeyAuth;
    }

    @Override
    public SecurityTokenType getType() {
        return securityTokenType;
    }

    @Override
    public String getUsername() {
        return publicKeyAuth == null ? null : publicKeyAuth.getUserName();
    }

    public String getPublicKey() {
        return publicKeyAuth == null ? null : publicKeyAuth.getPublicKey();
    }

    //- PRIVATE

    private final SecurityTokenType securityTokenType;
    private final SshKnob.PublicKeyAuthentication publicKeyAuth;
}
