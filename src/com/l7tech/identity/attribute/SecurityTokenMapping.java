package com.l7tech.identity.attribute;

import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.token.SecurityToken;

public abstract class SecurityTokenMapping extends AttributeMapping {
    private SecurityTokenType tokenType;

    public SecurityTokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(SecurityTokenType tokenType) {
        this.tokenType = tokenType;
    }

    public abstract String extractValue(SecurityToken creds);
}
