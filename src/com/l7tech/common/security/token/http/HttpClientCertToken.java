package com.l7tech.common.security.token.http;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;

public class HttpClientCertToken implements SecurityToken {
    public SecurityTokenType getType() {
        return SecurityTokenType.HTTP_CLIENT_CERT;
    }
}
