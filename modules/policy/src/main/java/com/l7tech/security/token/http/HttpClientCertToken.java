package com.l7tech.security.token.http;

import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.SecurityTokenType;

public class HttpClientCertToken implements SecurityToken {
    public SecurityTokenType getType() {
        return SecurityTokenType.HTTP_CLIENT_CERT;
    }
}
