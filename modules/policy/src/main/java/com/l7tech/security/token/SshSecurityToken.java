package com.l7tech.security.token;

import com.l7tech.message.SshKnob;

import java.net.PasswordAuthentication;

/**
 * SSH security token using user name, password and public key credential.
 */
public class SshSecurityToken implements SecurityToken, HasUsernameAndPassword {

    //- PUBLIC

    public SshSecurityToken(final SecurityTokenType securityTokenType,
                            final PasswordAuthentication passwordAuth,
                            final SshKnob.PublicKeyAuthentication publicKeyAuth) {
        this.securityTokenType = securityTokenType;
        this.passwordAuth = passwordAuth;
        this.publicKeyAuth = publicKeyAuth;
    }

    @Override
    public SecurityTokenType getType() {
        return securityTokenType;
    }

    @Override
    public String getUsername() {
        String userName = null;
        if (publicKeyAuth != null) {
            userName = publicKeyAuth.getUserName();
        } else if (passwordAuth != null) {
            userName = passwordAuth.getUserName();
        }
        return userName;
    }

    @Override
    public char[] getPassword() {
        return passwordAuth == null ? null : passwordAuth.getPassword();
    }

    public String getPublicKey() {
        return publicKeyAuth == null ? null : publicKeyAuth.getPublicKey();
    }

    //- PRIVATE

    private final SecurityTokenType securityTokenType;
    private final PasswordAuthentication passwordAuth;
    private final SshKnob.PublicKeyAuthentication publicKeyAuth;
}
