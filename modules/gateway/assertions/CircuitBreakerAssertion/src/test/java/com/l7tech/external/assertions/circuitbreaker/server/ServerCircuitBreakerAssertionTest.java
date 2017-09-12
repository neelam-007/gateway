package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.util.MockInjector;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.TestTimeSource;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.*;
import static org.junit.Assert.assertEquals;

/**
 * Test the CircuitBreakerAssertion.
 */
public class ServerCircuitBreakerAssertionTest {

    private static final String POLICY_FAILURE_CIRCUIT_TRACKER_ID = "policyFailureCircuitTrackerId";
    private static final String LATENCY_CIRCUIT_TRACKER_ID = "latencyCircuitTrackerId";
    private static final String POLICY_FAILURE_CIRCUIT_TRACKER_ID_CONTEXT_VAR = "policyFailureCircuitTrackerIdContextVar";
    private static final String POLICY_FAILURE_CIRCUIT_TRACKER_ID_CONTEXT_VAR_VALUE = "policyFailureCircuitTrackerIdContextVarValue";
    private static final String POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR = "policyFailureCircuitTrackerIdContextVar";
    private static final String POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR_VALUE = "4";
    private static final String POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR_INVALID_VALUE = "-4";

    private static final String LATENCY_CIRCUIT_TRACKER_ID_CONTEXT_VAR = "latencyCircuitTrackerIdContextVar";
    private static final String LATENCY_CIRCUIT_TRACKER_ID_CONTEXT_VAR_VALUE = "latencyCircuitTrackerIdContextVarValue";
    private static final String LATENCY_CIRCUIT_MAX_FAILURE_CONTEXT_VAR = "latencyCircuitTrackerIdContextVar";
    private static final String LATENCY_CIRCUIT_MAX_FAILURE_CONTEXT_VAR_VALUE = "4";
    private static final String LATENCY_CIRCUIT_MAX_FAILURE_CONTEXT_VAR_INVALID_VALUE = "-4";

    private ServerPolicyFactory serverPolicyFactory;
    private static TestTimeSource timeSource;
    private ServerConfig serverConfig;
    private static TestAudit testAudit;

    @Before
    public void setUp() throws Exception {
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        serverPolicyFactory.setApplicationContext(applicationContext);
        long startTime = CB_LATENCY_CIRCUIT_SAMPLING_WINDOW_DEFAULT + CB_POLICY_FAILURE_CIRCUIT_SAMPLING_WINDOW_DEFAULT;
        timeSource = new TestTimeSource(startTime, TimeUnit.MILLISECONDS.toNanos(startTime));
        serverConfig = new ServerConfigStub();
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

        assertion.setPolicyFailureCircuitEnabled(false);
        assertion.setLatencyCircuitEnabled(false);

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

        executePolicyFailureBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, false, null, null);
        testAudit.reset();
        //With custom tracker id
        executePolicyFailureBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, true, null, null);
        testAudit.reset();
        //With custom tracker id context variable need to fix
//        executePolicyFailureBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, true, true);
    }

    @Test
    public void testChildReturnsBAD_REQUESTRepeatedly_CircuitOpens_NextExecutionShortCircuits_CircuitClosesAfterRecoveryPeriod() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion = executePolicyFailureBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, false, null, null);
        timeSource.advanceByNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(CB_POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD_DEFAULT) + 10L);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(2, AssertionMessages.CB_CIRCUIT_TRIPPED, AssertionMessages.CB_CIRCUIT_OPEN);
    }

    @Test
    public void testChildReturnsBAD_REQUESTRepeatedly_CircuitOpens_NextExecutionShortCircuits_CircuitClosesAfterRecoveryPeriod_WithCustomTrackerId() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion = executePolicyFailureBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, true, null, null);
        timeSource.advanceByNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(CB_POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD_DEFAULT) + 10L);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(2, AssertionMessages.CB_CIRCUIT_TRIPPED, AssertionMessages.CB_CIRCUIT_OPEN);
    }

    @Test
    public void testChildFinishesBeforeLatencyPeriod_Repeatedly_CircuitStaysClosed() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;

        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(10L, returnStatus));

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);

        for (int executionNumber = 0; executionNumber < CB_LATENCY_CIRCUIT_MAX_FAILURES_DEFAULT + 2; executionNumber++) {
            assertEquals(returnStatus, serverAssertion.checkRequest(context));
        }

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        executeTestsForAudits(0);
    }

    @Test
    public void testChildFinishesAfterLatencyPeriod_Repeatedly_CircuitOpens_NextExecutionShortCircuits() throws Exception {
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        executeLatencyBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, false, null, null);
        testAudit.reset();
        //With custom tracker id
        executeLatencyBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, true, null, null);
        testAudit.reset();
        //With custom tracker id context var need to fix
//        executeLatencyBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, true, true);
    }

    @Test
    public void testChildFinishesAfterLatencyPeriod_Repeatedly_CircuitOpens_NextExecutionShortCircuits_CircuitClosesAfterRecoveryPeriod() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion = executeLatencyBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, false, null, null);
        timeSource.advanceByNanos(TimeUnit.MILLISECONDS.toNanos(CB_LATENCY_CIRCUIT_RECOVERY_PERIOD_DEFAULT) + 10L);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(2, AssertionMessages.CB_CIRCUIT_TRIPPED, AssertionMessages.CB_CIRCUIT_OPEN);
    }

    @Test
    public void testChildFinishesAfterLatencyPeriod_Repeatedly_CircuitOpens_NextExecutionShortCircuits_CircuitClosesAfterRecoveryPeriod_WithCustomTrackerId() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion = executeLatencyBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, true, null, null);
        timeSource.advanceByNanos(TimeUnit.MILLISECONDS.toNanos(CB_LATENCY_CIRCUIT_RECOVERY_PERIOD_DEFAULT) + 10L);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(2, AssertionMessages.CB_CIRCUIT_TRIPPED, AssertionMessages.CB_CIRCUIT_OPEN);
    }

    @Test
    public void testPolicyCircuitDisabled_ChildReturnsFailure_CircuitStillClosed() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = executePolicyFailureBasedOnFlagAndGetServerCircuitBreakerAssertion(context, false, false, null, null);
        timeSource.advanceByNanos(TimeUnit.MILLISECONDS.toNanos(10));

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(0);
    }

    @Test
    public void testLatencyCircuitDisabled_ChildFinishesAfterLatencyPeriod_CircuitStillClosed() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = executeLatencyBasedOnFlagAndGetServerCircuitBreakerAssertion(context, false, false, null, null);
        timeSource.advanceByNanos(TimeUnit.MILLISECONDS.toNanos(CB_LATENCY_CIRCUIT_RECOVERY_PERIOD_DEFAULT) + 10L);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(0);
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

        for (int executionNumber = 0; executionNumber < CB_POLICY_FAILURE_CIRCUIT_MAX_FAILURES_DEFAULT; executionNumber++) {
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

        for (int executionNumber = 0; executionNumber < CB_POLICY_FAILURE_CIRCUIT_MAX_FAILURES_DEFAULT; executionNumber++) {
            assertEquals(badReturnStatus, serverAssertion.checkRequest(context));
        }

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        executeTestsForAudits(2, AssertionMessages.CB_CIRCUIT_TRIPPED, AssertionMessages.CB_CIRCUIT_OPEN);
    }

    @Test
    public void testPolicyMaxFailureWithContextVarSuccess() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionForPolicyFailure(context, returnStatus, true, false, false, POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR, POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR_VALUE);
        for (int i = 0; i < 3; i++) {
            assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
            executeTestsForAudits(0);
        }
    }

    @Test
    public void testChildReturnsBAD_REQUESTRepeatedly_CircuitOpens_NextExecutionShortCircuits_CircuitClosesAfterRecoveryPeriod_WithMaxFailureContextVar() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion = executePolicyFailureBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, false, POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR, POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR_VALUE);
        timeSource.advanceByNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(CB_POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD_DEFAULT) + 10L);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(2, AssertionMessages.CB_CIRCUIT_TRIPPED, AssertionMessages.CB_CIRCUIT_OPEN);
    }

    @Test
    public void testPolicyMaxFailureWithContextVarFailure() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionForPolicyFailure(context, returnStatus, true, false, false, POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR, POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR_INVALID_VALUE);
        for (int i = 0; i < 3; i++) {
            try {
                assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
            } catch(AssertionStatusException e) {
                assertEquals(AssertionStatus.SERVER_ERROR, e.getAssertionStatus());
                executeTestsForAudits(1, AssertionMessages.CB_CIRCUIT_INVALID_CONFIGURATION);
            }
            testAudit.reset();
        }
    }

    @Test
    public void testLatencyMaxFailureWithContextVarSuccess() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionForPolicyFailure(context, returnStatus, true, false, false, POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR, POLICY_FAILURE_CIRCUIT_MAX_FAILURE_CONTEXT_VAR_VALUE);
        for (int i = 0; i < 3; i++) {
            assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
            executeTestsForAudits(0);
        }
    }

    @Test
    public void testChildFinishesAfterLatencyPeriod_Repeatedly_CircuitOpens_NextExecutionShortCircuits_CircuitClosesAfterRecoveryPeriod_WithMaxFailureContextVar() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();

        ServerCircuitBreakerAssertion serverAssertion = executeLatencyBasedOnFlagAndGetServerCircuitBreakerAssertion(context, true, false, LATENCY_CIRCUIT_MAX_FAILURE_CONTEXT_VAR, LATENCY_CIRCUIT_MAX_FAILURE_CONTEXT_VAR_VALUE);
        timeSource.advanceByNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(CB_LATENCY_CIRCUIT_RECOVERY_PERIOD_DEFAULT) + 10L);

        assertEquals(returnStatus, serverAssertion.checkRequest(context));
        executeTestsForAudits(2, AssertionMessages.CB_CIRCUIT_TRIPPED, AssertionMessages.CB_CIRCUIT_OPEN);
    }

    @Test
    public void testLatencyMaxFailureWithContextVarFailure() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionForLatency(context, returnStatus, true, false, false, LATENCY_CIRCUIT_MAX_FAILURE_CONTEXT_VAR, LATENCY_CIRCUIT_MAX_FAILURE_CONTEXT_VAR_INVALID_VALUE);
        for (int i = 0; i < 3; i++) {
            try {
                assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
            } catch(AssertionStatusException e) {
                assertEquals(AssertionStatus.SERVER_ERROR, e.getAssertionStatus());
                executeTestsForAudits(1, AssertionMessages.CB_CIRCUIT_INVALID_CONFIGURATION);
            }
            testAudit.reset();
        }
    }

    @Test
    public void testPolicyTrackerIdWithContextVarSuccess() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionForPolicyFailure(context, returnStatus, true, true, true, null, null);
        for (int i = 0; i < 3; i++) {
            assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
            executeTestsForAudits(0);
        }
    }

    @Test
    public void testLatencyTrackerIdWithContextVarSuccess() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionForLatency(context, returnStatus, true, true, true, null, null);
        for (int i = 0; i < 3; i++) {
            assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
            executeTestsForAudits(0);
        }
    }

    @Test
    public void testPolicyTrackerIdWithInvalidContextVarFailsWithAssertionStatusException() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionForPolicyFailure(context, returnStatus, true, true, true, null, null);
        for (int i = 0; i < 3; i++) {
            try {
                assertEquals(returnStatus, serverAssertion.checkRequest(context));
            } catch(AssertionStatusException e) {
                assertEquals(AssertionStatus.SERVER_ERROR, e.getAssertionStatus());
                executeTestsForAudits(3, AssertionMessages.CB_CIRCUIT_INVALID_CONFIGURATION, AssertionMessages.NO_SUCH_VARIABLE, CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE);
            }
            testAudit.reset();
        }
    }

    @Test
    public void testLatencyTrackerIdWithInvalidContextVarFailsWithAssertionStatusException() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionForLatency(context, returnStatus, true, true, true, null, null);
        for (int i = 0; i < 3; i++) {
            try {
                assertEquals(returnStatus, serverAssertion.checkRequest(context));
            } catch(AssertionStatusException e) {
                assertEquals(AssertionStatus.SERVER_ERROR, e.getAssertionStatus());
                executeTestsForAudits(3, AssertionMessages.CB_CIRCUIT_INVALID_CONFIGURATION, AssertionMessages.NO_SUCH_VARIABLE, CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE);
            }
            testAudit.reset();
        }
    }

    @Test
    public void testDisableBothCircuitsWithChildPolicySuccess_WithLatency() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.NONE;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionWithBothCircuitsDisabled(returnStatus);
        for (int i = 0; i < 10; i++) {
            assertEquals(returnStatus, serverAssertion.checkRequest(context));
            executeTestsForAudits(0);
            testAudit.reset();
        }
    }

    @Test
    public void testDisableBothCircuitsWithChildPolicyFailureAndLatency() throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;
        PolicyEnforcementContext context = createPolicyEnforcementContext();
        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionWithBothCircuitsDisabled(returnStatus);
        for (int i = 0; i < 10; i++) {
            assertEquals(returnStatus, serverAssertion.checkRequest(context));
            executeTestsForAudits(0);
            testAudit.reset();
        }
    }

    private ServerCircuitBreakerAssertion executePolicyFailureBasedOnFlagAndGetServerCircuitBreakerAssertion(
            final PolicyEnforcementContext context, final boolean policyCircuitEnabled, final boolean isCustomTrackerIdEnabled,
            final String maxFailureContextVar, final String maxFailureValue) throws Exception {
        final AssertionStatus returnStatus = AssertionStatus.BAD_REQUEST;

        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionForPolicyFailure(context, returnStatus, policyCircuitEnabled, isCustomTrackerIdEnabled, false, maxFailureContextVar, maxFailureValue);

        int maxExecution = CB_POLICY_FAILURE_CIRCUIT_MAX_FAILURES_DEFAULT;
        if (StringUtils.isNotEmpty(maxFailureValue)) {
            try {
                maxExecution = Integer.parseInt(maxFailureValue);
            } catch(NumberFormatException e) {
                //do nothing for now
            }
        }

        for (int executionNumber = 0; executionNumber < maxExecution; executionNumber++) {
            assertEquals(returnStatus, serverAssertion.checkRequest(context));
        }
        if (policyCircuitEnabled) {
            assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
            executeTestsForAudits(2, AssertionMessages.CB_CIRCUIT_TRIPPED, AssertionMessages.CB_CIRCUIT_OPEN);
        } else {
            assertEquals(returnStatus, serverAssertion.checkRequest(context));
            executeTestsForAudits(0);
        }
        return serverAssertion;
    }

    private ServerCircuitBreakerAssertion executeLatencyBasedOnFlagAndGetServerCircuitBreakerAssertion(
            final PolicyEnforcementContext context, final boolean latencyCircuitEnabled, final boolean isCustomTrackerIdEnabled,
            final String maxFailureContextVar, final String maxFailureValue) throws IOException, PolicyAssertionException, LicenseException {
        final AssertionStatus returnStatus = AssertionStatus.NONE;

        ServerCircuitBreakerAssertion serverAssertion = getServerCircuitBreakerAssertionForLatency(context, returnStatus, latencyCircuitEnabled, isCustomTrackerIdEnabled, false, maxFailureContextVar, maxFailureValue);

        int maxExecution = CB_LATENCY_CIRCUIT_MAX_FAILURES_DEFAULT;
        if (StringUtils.isNotEmpty(maxFailureValue)) {
            try {
                maxExecution = Integer.parseInt(maxFailureValue);
            } catch(NumberFormatException e) {
                //do nothing for now
            }
        }

        for (int executionNumber = 0; executionNumber < maxExecution; executionNumber++) {
            assertEquals(returnStatus, serverAssertion.checkRequest(context));
        }
        if (latencyCircuitEnabled) {
            assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
            executeTestsForAudits(2, AssertionMessages.CB_CIRCUIT_TRIPPED, AssertionMessages.CB_CIRCUIT_OPEN);
        } else {
            assertEquals(returnStatus, serverAssertion.checkRequest(context));
            executeTestsForAudits(0);
        }
        return serverAssertion;
    }

    //make sure customtrackerid is enabled to use context variable for tracker id.
    private ServerCircuitBreakerAssertion getServerCircuitBreakerAssertionForPolicyFailure(
            final PolicyEnforcementContext context, AssertionStatus returnStatus, final boolean policyCircuitEnabled,
            final boolean isCustomTrackerIdEnabled, final boolean useContextVariable, final String maxFailureContextVar, final String maxFailureValue) throws ServerPolicyException, LicenseException {
        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(10L, returnStatus));
        assertion.setPolicyFailureCircuitEnabled(policyCircuitEnabled);
        assertion.setPolicyFailureCircuitCustomTrackerIdEnabled(isCustomTrackerIdEnabled);
        if (isCustomTrackerIdEnabled) {
            if (useContextVariable) {
                assertion.setPolicyFailureCircuitTrackerId(Syntax.SYNTAX_PREFIX + POLICY_FAILURE_CIRCUIT_TRACKER_ID_CONTEXT_VAR + Syntax.SYNTAX_SUFFIX);
                context.setVariable(POLICY_FAILURE_CIRCUIT_TRACKER_ID_CONTEXT_VAR, POLICY_FAILURE_CIRCUIT_TRACKER_ID_CONTEXT_VAR_VALUE);
            } else {
                assertion.setPolicyFailureCircuitTrackerId(POLICY_FAILURE_CIRCUIT_TRACKER_ID);
            }
        }
        if (StringUtils.isNotEmpty(maxFailureContextVar)) {
            assertion.setPolicyFailureCircuitMaxFailures(Syntax.SYNTAX_PREFIX + maxFailureContextVar + Syntax.SYNTAX_SUFFIX);
            context.setVariable(maxFailureContextVar, maxFailureValue);
        }

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);
        return serverAssertion;
    }

    //make sure customtrackerid is enabled to use context variable for tracker id.
    private ServerCircuitBreakerAssertion getServerCircuitBreakerAssertionForLatency(
            final PolicyEnforcementContext context, AssertionStatus returnStatus, final boolean latencyCircuitEnabled,
            final boolean isCustomTrackerIdEnabled, final boolean useContextVariableForTrackerId, final String maxFailureContextVar, final String maxFailureValue) throws ServerPolicyException, LicenseException {
        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(TimeUnit.MILLISECONDS.toNanos(CB_LATENCY_CIRCUIT_MAX_LATENCY_DEFAULT) + 10L, returnStatus));
        assertion.setLatencyCircuitEnabled(latencyCircuitEnabled);
        assertion.setLatencyCircuitCustomTrackerIdEnabled(isCustomTrackerIdEnabled);
        if (isCustomTrackerIdEnabled) {
            if (useContextVariableForTrackerId) {
                assertion.setLatencyCircuitTrackerId(Syntax.SYNTAX_PREFIX + LATENCY_CIRCUIT_TRACKER_ID_CONTEXT_VAR + Syntax.SYNTAX_SUFFIX);
                context.setVariable(LATENCY_CIRCUIT_TRACKER_ID_CONTEXT_VAR, LATENCY_CIRCUIT_TRACKER_ID_CONTEXT_VAR_VALUE);
            } else {
                assertion.setLatencyCircuitTrackerId(LATENCY_CIRCUIT_TRACKER_ID);
            }
        }
        if (StringUtils.isNotEmpty(maxFailureContextVar)) {
            assertion.setLatencyCircuitMaxFailures(Syntax.SYNTAX_PREFIX + maxFailureContextVar + Syntax.SYNTAX_SUFFIX);
            context.setVariable(maxFailureContextVar, maxFailureValue);
        }

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);
        return serverAssertion;
    }

    private ServerCircuitBreakerAssertion getServerCircuitBreakerAssertionWithBothCircuitsDisabled(AssertionStatus returnStatus) throws ServerPolicyException, LicenseException {
        final CircuitBreakerAssertion assertion = new CircuitBreakerAssertion(getMockAssertions(TimeUnit.MILLISECONDS.toNanos(CB_LATENCY_CIRCUIT_MAX_LATENCY_DEFAULT) + 10L, returnStatus));
        assertion.setPolicyFailureCircuitEnabled(false);
        assertion.setLatencyCircuitEnabled(false);

        ServerCircuitBreakerAssertion serverAssertion =
                (ServerCircuitBreakerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        injectDependencies(serverAssertion);
        return serverAssertion;
    }

    @NotNull
    private static List<Assertion> getMockAssertions(final long executionTime, final AssertionStatus... returnStatus) {
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

    private static void executeTestsForAudits(int numberOfAudits, Messages.M... messages) {
        assertEquals(numberOfAudits, testAudit.getAuditCount());
        if (numberOfAudits > 0 && messages.length > 0) {
            for (Messages.M message : messages) {
                assertEquals(Boolean.TRUE, testAudit.isAuditPresent(message));
            }
        }
    }

    private void injectDependencies(ServerCircuitBreakerAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .put("timeSource", timeSource)
                .put("serverConfig", serverConfig)
                .unmodifiableMap()
        );
    }
}
