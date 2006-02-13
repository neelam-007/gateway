package com.l7tech.identity.mapping;

import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.token.SecurityToken;

/**
 * Describes how the attribute described by an {@link AttributeConfig} is implmented in a particular
 * type of {@link SecurityToken}.
 */
public abstract class SecurityTokenMapping extends AttributeMapping {
    private SecurityTokenType tokenType;

    public SecurityTokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(SecurityTokenType tokenType) {
        this.tokenType = tokenType;
    }

    public abstract Object[] extractValues(SecurityToken creds);
}
