package com.l7tech.server.event.identity;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationEvent;

public class FailedAuthentication extends ApplicationEvent {
    private LoginCredentials credentials;

    public FailedAuthentication(ServerAssertion assertion, LoginCredentials creds) {
        super(assertion);
        this.credentials = creds;
    }
}
