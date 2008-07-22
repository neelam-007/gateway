package com.l7tech.security.token.http;

import com.l7tech.security.token.*;

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
