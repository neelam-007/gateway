package com.l7tech.security.token.http;

import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.HasUsername;
import com.l7tech.security.token.SecurityTokenType;

import java.util.*;

public class HttpDigestToken implements SecurityToken, HasUsername {
    private final String username;
    private final String ha1hex;
    private final String realm;
    private final Map<String,String> params;

    public HttpDigestToken(String username, String ha1, String realm, Map<String,String> params) {
        this.username = username;
        this.ha1hex = ha1;
        this.realm = realm;
        this.params = params==null ?
                Collections.<String,String>emptyMap() :
                Collections.unmodifiableMap(new HashMap<String,String>(params));
    }

    public HttpDigestToken(String username, String ha1) {
        this(username, ha1, null, null);
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.HTTP_DIGEST;
    }

    public String getUsername() {
        return username;
    }

    public String getHa1Hex() {
        return ha1hex;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getRealm() {
        return realm;
    }
}
