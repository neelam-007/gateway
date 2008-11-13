/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.credential.wss.WssCredentialSourceAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;
import org.springframework.context.ApplicationContext;

public abstract class ServerWssCredentialSource extends ServerCredentialSourceAssertion<WssCredentialSourceAssertion> implements ServerAssertion {
    public ServerWssCredentialSource( WssCredentialSourceAssertion data, ApplicationContext springContext ) {
        super( data, springContext );
    }

    protected void challenge(PolicyEnforcementContext context) {
        // Meaningless for WS-Security
    }
}
