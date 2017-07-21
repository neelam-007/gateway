package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * Test the CircuitBreakerAssertion.
 */
public class ServerCircuitBreakerAssertionTest {

    private final ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
    
    @Test
    public void testEmptyCircuitBreakerSucceeds() throws Exception {
        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(new ByteArrayStashManager(),
                                ContentTypeHeader.XML_DEFAULT, new EmptyInputStream()),
                        new Message(),
                        false);

        CircuitBreakerAssertion circuitBreakerAssertion = new CircuitBreakerAssertion();
        ServerCircuitBreakerAssertion serverCircuitBreakerAssertion = new ServerCircuitBreakerAssertion(circuitBreakerAssertion, applicationContext);
        assertEquals(AssertionStatus.NONE, serverCircuitBreakerAssertion.checkRequest(context));
    }
}
