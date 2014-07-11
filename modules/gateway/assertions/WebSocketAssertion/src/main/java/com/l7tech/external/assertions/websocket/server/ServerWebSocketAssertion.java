package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

/**
 * Server side implementation of the WebSocketOutboundAssertion.
 *
 * @see com.l7tech.external.assertions.websocket.WebSocketAssertion
 */
public class ServerWebSocketAssertion extends AbstractServerAssertion<WebSocketAssertion> {
    private final String[] variablesUsed;

    public ServerWebSocketAssertion( final WebSocketAssertion assertion ) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        return AssertionStatus.NONE;
    }

}
