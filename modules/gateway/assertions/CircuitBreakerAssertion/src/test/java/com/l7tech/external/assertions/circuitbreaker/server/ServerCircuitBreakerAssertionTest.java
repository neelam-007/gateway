package com.l7tech.external.assertions.circuitbreaker.server;

import static org.junit.Assert.*;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.composite.ServerAllAssertion;
import com.l7tech.test.BugId;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.logging.Logger;

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

        AllAssertion all = new AllAssertion(Collections.emptyList());
        ServerAllAssertion sAll = new ServerAllAssertion(all, applicationContext);
        assertEquals(AssertionStatus.NONE, sAll.checkRequest(context));
    }

}
