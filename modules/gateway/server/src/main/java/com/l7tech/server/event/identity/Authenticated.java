package com.l7tech.server.event.identity;

import org.springframework.context.ApplicationEvent;
import com.l7tech.server.identity.AuthenticationResult;

public class Authenticated extends ApplicationEvent {
    private final AuthenticationResult authenticationResult;

    public Authenticated(AuthenticationResult result) {
        super(result);
        this.authenticationResult = result;
    }

    public AuthenticationResult getAuthenticationResult() {
        return authenticationResult;
    }
}
