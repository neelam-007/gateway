package com.l7tech.security.token.http;

import com.l7tech.security.token.*;

import java.net.PasswordAuthentication;

public class HttpBasicToken extends UsernamePasswordSecurityToken {

    public HttpBasicToken( final PasswordAuthentication passwordAuth ) {
        super( SecurityTokenType.HTTP_BASIC, passwordAuth);
    }

    public HttpBasicToken( final String username,
                           final char[] password ) {
        super( SecurityTokenType.HTTP_BASIC, username, password);
    }
}
