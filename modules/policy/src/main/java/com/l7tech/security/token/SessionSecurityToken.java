package com.l7tech.security.token;

/**
 * Security token that represents an internal session.
 */
public class SessionSecurityToken implements SecurityToken {

    private final SecurityTokenType type;
    private final long providerId;
    private final String userId;
    private final String login;

    public SessionSecurityToken( final SecurityTokenType type,
                                 final long providerId,
                                 final String userId,
                                 final String login ) {
        this.type = type;
        this.providerId = providerId;
        this.userId = userId;
        this.login = login;
    }

    public long getProviderId() {
        return providerId;
    }

    public String getUserId() {
        return userId;
    }

    public String getLogin() {
        return login;
    }

    @Override
    public SecurityTokenType getType() {
        return type;
    }
}
