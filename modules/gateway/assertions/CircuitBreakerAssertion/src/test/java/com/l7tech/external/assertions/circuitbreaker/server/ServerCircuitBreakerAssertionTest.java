package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.util.MockInjector;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.TestTimeSource;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test the CircuitBreakerAssertion.
 */
public class ServerCircuitBreakerAssertionTest {

    private ServerPolicyFactory serverPolicyFactory;
    private static TestTimeSource timeSource;
    private static TestAudit testAudit;

    @Before
    public void setUp() throws Exception {
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        serverPolicyFactory.setApplicationContext(applicationContext);
        long startTime = CircuitBreakerAssertion.LATENCY_FAILURE_SAMPLING_WINDOW + CircuitBreakerAssertion.POLICY_FAILURE_SAMPLING_WINDOW;
        timeSource = new TestTimeSource(startTime, TimeUnit.MILLISECONDS.toNanos(startTime));
        testAudit = new TestAudit();
        CircuitStateManagerHolder.setCircuitStateManager(new CircuitStateManager());
        EventTrackerManagerHolder.setEventTrackerManager(new EventTrackerManager());
    }

    @Test
    public void testEmptyCircuitBreaker_WithPolicyAndLatencyConfigured_Succeeds() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(Collections.emptyList());

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(0);
    }

    @Test
    public void testEmptyCircuitBreaker_WithoutPolicyAndLatencyConfigured_Succeeds() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(Collections.emptyList());

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        assertion.setPolicyFailureEnabled(false);
        assertion.setLatencyFailureEnabled(false);

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(0);
    }

    @Test
    public void testChildPolicyReturnsNONE_AssertionReturnsNONE() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(10L, returnStatus));

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(0);
    }

    @Test
    public void testChildPolicyReturnsBAD_REQUEST_AssertionReturnsBAD_REQUEST() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(10L, returnStatus));

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(0);
    }

    @Test
    public void testChildReturnsBAD_REQUESTRepeatedly_CircuitOpens_NextExecutionShortCircuits() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        executePolicyFailureAndGetServerCircuitBreakerAssertion(context);
    }

    @Test
    public void testChildReturnsBAD_REQUESTRepeatedly_CircuitOpens_NextExecutionShortCircuits_CircuitClosesAfterRecoveryPeriod() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = executePolicyFailureAndGetServerCircuitBreakerAssertion(context);
        timeSource.advanceByNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(CircuitBreakerAssertion.POLICY_FAILURE_RECOVERY_PERIOD) + 10L);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(1, AssertionMessages.CB_CIRCUIT_TRIPPED);
    }

    @Test
    public void testChildFinishesBeforeLatencyPeriod_Repeatedly_CircuitStaysClosed() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(10L, returnStatus));

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);

        for(int executionNumber = 0; executionNumber < CircuitBreakerAssertion.LATENCY_FAILURE_MAX_COUNT + 2; executionNumber++) {
            assertEquals(returnStatus, serverAssertion.checkRequest(context));
        }

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        executeTestsForAudits(0);
    }

    @Test
    public void testChildFinishesAfterLatencyPeriod_Repeatedly_CircuitOpens_NextExecutionShortCircuits() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        executeLatencyFailureAndGetServerCircuitBreakerAssertion(context);
    }

    @Test
    public void testChildFinishesAfterLatencyPeriod_Repeatedly_CircuitOpens_NextExecutionShortCircuits_CircuitClosesAfterRecoveryPeriod() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = executeLatencyFailureAndGetServerCircuitBreakerAssertion(context);
        timeSource.advanceByNanos(TimeUnit.MILLISECONDS.toNanos(CircuitBreakerAssertion.LATENCY_FAILURE_RECOVERY_PERIOD) + 10L);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(1, AssertionMessages.CB_CIRCUIT_TRIPPED);
    }

    @Test
    public void testNestedCircuitBreakerAssertion_With_Success() throws Exception {
        final AssertionStatus goodReturnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        List<Assertion> childrenForNested = getMockAssertions(10L, goodReturnStatus, goodReturnStatus);

        final CircuitBreakerAssertion nestedAssertion = new CircuitBreakerAssertion(childrenForNested);
        childrenForNested.add(1, nestedAssertion);
        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(childrenForNested);

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);

        for(int executionNumber = 0; executionNumber < CircuitBreakerAssertion.POLICY_FAILURE_MAX_COUNT; executionNumber++) {
            assertEquals(goodReturnStatus, serverAssertion.checkRequest(context));
        }

        assertEquals(goodReturnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(0);
    }

    @Test
    public void testNestedCircuitBreakerAssertion_With_Policy_Failure() throws Exception {
        final AssertionStatus badReturnStatus = AssertionStatus.BAD_REQUEST;
        final AssertionStatus goodReturnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        List<Assertion> badAssertion = getMockAssertions(10L, badReturnStatus);
        List<Assertion> childrenForNested = getMockAssertions(10L, goodReturnStatus, goodReturnStatus);

        final CircuitBreakerAssertion nestedAssertion = new CircuitBreakerAssertion(childrenForNested);
        badAssertion.addAll(0, childrenForNested);
        badAssertion.add(0, nestedAssertion);
        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(badAssertion);

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);

        for(int executionNumber = 0; executionNumber < CircuitBreakerAssertion.POLICY_FAILURE_MAX_COUNT; executionNumber++) {
            assertEquals(badReturnStatus, serverAssertion.checkRequest(context));
        }

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        executeTestsForAudits(1, AssertionMessages.CB_CIRCUIT_TRIPPED);
    }

    private ServerCircuitBreakerAssertion executePolicyFailureAndGetServerCircuitBreakerAssertion(final PolicyEnforcementContext context) throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(10L, returnStatus));

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);

        for(int executionNumber = 0; executionNumber < CircuitBreakerAssertion.POLICY_FAILURE_MAX_COUNT; executionNumber++) {
            assertEquals(returnStatus, serverAssertion.checkRequest(context));
        }

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        executeTestsForAudits(1, AssertionMessages.CB_CIRCUIT_TRIPPED);
        return serverAssertion;
    }

    private ServerCircuitBreakerAssertion executeLatencyFailureAndGetServerCircuitBreakerAssertion(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException, LicenseException {
        final AssertionStatus returnStatus = AssertionStatus.NONE;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(TimeUnit.MILLISECONDS.toNanos(CircuitBreakerAssertion.LATENCY_FAILURE_LIMIT) + 10L, returnStatus));

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);

        for(int executionNumber = 0; executionNumber < CircuitBreakerAssertion.LATENCY_FAILURE_MAX_COUNT; executionNumber++) {
            assertEquals(returnStatus, serverAssertion.checkRequest(context));
        }

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        executeTestsForAudits(1, AssertionMessages.CB_CIRCUIT_TRIPPED);
        return serverAssertion;
    }

    @NotNull
    static List<Assertion> getMockAssertions(final long executionTime, final AssertionStatus... returnStatus) {
        ArrayList<Assertion> assertionList = new ArrayList<>();

        for (AssertionStatus status : returnStatus) {
            MockAssertion mockAssertion = new MockAssertion();
            mockAssertion.setReturnStatus(status);
            mockAssertion.setExecutionTime(executionTime);
            mockAssertion.setTimeSource(timeSource);
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

    static void executeTestsForAudits(int numberOfAudits, Messages.M... messages) {
        assertEquals(numberOfAudits, testAudit.getAuditCount());
        if(numberOfAudits > 0 && messages.length > 0) {
            for(Messages.M message : messages) {
                assertEquals(Boolean.TRUE, testAudit.isAuditPresent(message));
            }
        }
    }

    private static void injectDependencies(ServerCircuitBreakerAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .put("timeSource", timeSource)
                .unmodifiableMap()
        );
    }
}
