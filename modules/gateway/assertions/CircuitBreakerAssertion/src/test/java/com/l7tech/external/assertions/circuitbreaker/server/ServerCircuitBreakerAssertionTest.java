package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.util.MockInjector;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test the CircuitBreakerAssertion.
 */
public class ServerCircuitBreakerAssertionTest {

    private static ServerPolicyFactory serverPolicyFactory;

    @BeforeClass
    public static void init() {
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();

        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        serverPolicyFactory.setApplicationContext(applicationContext);
    }

    @Test
    public void testEmptyCircuitBreakerSucceeds() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(Collections.emptyList());

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
    }

    @Test
    public void testChildPolicyReturnsNONE_AssertionReturnsNONE() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(returnStatus));

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
    }

    @Test
    public void testChildPolicyReturnsBAD_REQUEST_AssertionReturnsBAD_REQUEST() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(returnStatus));

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
    }

    @Test
    public void testChildReturnsBAD_REQUESTRepeatedly_CircuitOpens_NextExecutionShortCircuits() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(returnStatus));

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion = 
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        
        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        assertEquals(returnStatus, serverAssertion.checkRequest(context));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
    }

    @NotNull
    private List<Assertion> getMockAssertions(AssertionStatus... returnStatus) {
        ArrayList<Assertion> assertionList = new ArrayList<>();

        for (AssertionStatus status : returnStatus) {
            MockAssertion mockAssertion = new MockAssertion();
            mockAssertion.setReturnStatus(status);
            assertionList.add(mockAssertion);
        }

        return assertionList;
    }

    private PolicyEnforcementContext createPolicyEnforcementContext() throws Exception {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(new ByteArrayStashManager(),
                                ContentTypeHeader.XML_DEFAULT, new EmptyInputStream()),
                        new Message(),
                        false);
    }
}
