package com.l7tech.security.token.http;

import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.HasUsername;
import com.l7tech.security.token.SecurityTokenType;

public class HttpDigestToken implements SecurityToken, HasUsername {
    private final String username;
    private final String ha1hex;

    public HttpDigestToken(String username, String ha1) {
        this.username = username;
        this.ha1hex = ha1;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.HTTP_DIGEST;
    }

    public String getUsername() {
        return username;
    }

    public String getHa1Hex() {
        return ha1hex;
    }
}
