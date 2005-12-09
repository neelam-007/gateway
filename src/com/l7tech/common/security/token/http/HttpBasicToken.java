package com.l7tech.common.security.token.http;

import com.l7tech.common.security.token.HasUsernameAndPassword;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;

import java.net.PasswordAuthentication;

public class HttpBasicToken implements SecurityToken, HasUsernameAndPassword {
    private final PasswordAuthentication passwordAuth;

    public HttpBasicToken(PasswordAuthentication passwordAuth) {
        this.passwordAuth = passwordAuth;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.HTTP_BASIC;
    }

    public String getUsername() {
        return passwordAuth.getUserName();
    }

    public char[] getPassword() {
        return passwordAuth.getPassword();
    }
}
