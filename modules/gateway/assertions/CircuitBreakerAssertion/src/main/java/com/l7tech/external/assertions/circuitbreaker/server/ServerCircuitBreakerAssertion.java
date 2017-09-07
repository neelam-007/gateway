package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.TimeSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.CIRCUIT_TYPE_LATENCY;
import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.CIRCUIT_TYPE_POLICY_FAILURE;

/**
 * Server side implementation of the CircuitBreakerAssertion.
 *
 * @see com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion
 */
public class ServerCircuitBreakerAssertion extends ServerCompositeAssertion<CircuitBreakerAssertion> {

    private static final Logger logger = Logger.getLogger(ServerCircuitBreakerAssertion.class.getName());
    private String policyFailureCircuitTrackerId;
    private String latencyCircuitTrackerId;

    private final String[] variablesUsed;

    private CircuitStateManager circuitStateManager = CircuitStateManagerHolder.getCircuitStateManager();
    private EventTrackerManager eventTrackerManager = EventTrackerManagerHolder.getEventTrackerManager();

    @Inject
    private TimeSource timeSource;

    private static SecureRandom secureRandom = JceProvider.getInstance().getSecureRandom();

    private final AssertionResultListener assertionResultListener = (context, result) -> {
        if (result != AssertionStatus.NONE) {
            seenAssertionStatus(context, result);
            rollbackDeferredAssertions(context);
            return false;
        }
        return true;
    };

    public ServerCircuitBreakerAssertion(final CircuitBreakerAssertion assertion, ApplicationContext applicationContext) throws PolicyAssertionException, LicenseException, NoSuchAlgorithmException {
        super(assertion, applicationContext);
        this.variablesUsed = assertion.getVariablesUsed();
        if (!assertion.isPolicyFailureCircuitCustomTrackerIdEnabled()) {
            policyFailureCircuitTrackerId = "policy_" + String.valueOf(Math.abs(secureRandom.nextInt()));
        }

        if (!assertion.isLatencyCircuitCustomTrackerIdEnabled()) {
            latencyCircuitTrackerId = "latency_" + String.valueOf(Math.abs(secureRandom.nextInt()));
        }
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Map<String,Object> variableMap = context.getVariableMap(variablesUsed, getAudit());

        // check if the circuit is open and if so, has the recovery period been exceeded yet
        CircuitConfig policyFailureCircuit = getFailureCircuitConfig(variableMap);
        CircuitConfig latencyCircuit = getLatencyCircuitConfig(variableMap);

        if (((assertion.isPolicyFailureCircuitCustomTrackerIdEnabled() || assertion.isLatencyCircuitCustomTrackerIdEnabled())
                && isAnyCircuitForcedOpen(policyFailureCircuit, latencyCircuit)) ||
                !isCircuitClosed(policyFailureCircuit, assertion.isPolicyFailureCircuitCustomTrackerIdEnabled()) ||
                !isCircuitClosed(latencyCircuit, assertion.isLatencyCircuitCustomTrackerIdEnabled())) {
            return AssertionStatus.FALSIFIED;
        }

        long executionStartTimestamp = timeSource.nanoTime();
        // run child policy
        final AssertionStatus status = iterateChildren(context, assertionResultListener);
        long executionEndTimestamp = timeSource.nanoTime();
        long currentExecutionLatency = executionEndTimestamp - executionStartTimestamp;

        //handle policy failures and latency
        if (null != policyFailureCircuit  && AssertionStatus.NONE != status) {
            //On shared tracker, in order avoid overriding policy failures events with latency events, we are reducing by 1 nano sec.
            updateEventTracker(policyFailureCircuit, (executionEndTimestamp - 1L));
        }
        if (null != latencyCircuit && currentExecutionLatency > TimeUnit.MILLISECONDS.toNanos(((Latency)latencyCircuit.getFailureCondition()).getLimit())) {
            updateEventTracker(latencyCircuit, executionEndTimestamp);

            // log latency event with latency time
            logLatencyLimitExceededEvent(latencyCircuit.getTrackerId(), currentExecutionLatency);
        }

       return status;
    }

    private void updateEventTracker(final CircuitConfig circuitConfig, final long timeStamp) {
        EventTracker eventTracker = getEventTracker(circuitConfig.getTrackerId());
        eventTracker.recordEvent(timeStamp);

        long sinceTimeStamp = timeStamp - TimeUnit.MILLISECONDS.toNanos(
                circuitConfig.getFailureCondition().getSamplingWindow());
        long count = eventTracker.getCountSinceTimestamp(sinceTimeStamp);
        logger.log(Level.FINE, String.format("Event count is %s. Configured max failures is %s for trackerID %s", count,
                circuitConfig.getFailureCondition().getMaxFailureCount(), circuitConfig.getTrackerId()));
        
        // open circuit if failure count is more than threshold
        if (count >= circuitConfig.getFailureCondition().getMaxFailureCount()) {
            openCircuit(circuitConfig);
        }
    }

    private void openCircuit(final CircuitConfig circuitConfig) {
        long circuitOpenUntil = timeSource.currentTimeMillis() + circuitConfig.getRecoveryPeriod();
        circuitStateManager.addState(circuitConfig, circuitOpenUntil);
        auditCircuitTripped(circuitConfig);
    }

    private CircuitConfig getFailureCircuitConfig(final Map<String, Object> variableMap) {
        if (assertion.isPolicyFailureCircuitEnabled()) {
            return new CircuitConfig(getConfiguredValue(assertion.isPolicyFailureCircuitCustomTrackerIdEnabled() ? assertion.getPolicyFailureCircuitTrackerId() : policyFailureCircuitTrackerId, variableMap, CIRCUIT_TYPE_POLICY_FAILURE),
                    getConfiguredValueFromSingleContextVariable(assertion.getPolicyFailureCircuitRecoveryPeriod(), variableMap, CIRCUIT_TYPE_POLICY_FAILURE, "Recovery Period"),
                    new PolicyFailure(getConfiguredValueFromSingleContextVariable(assertion.getPolicyFailureCircuitSamplingWindow(), variableMap, CIRCUIT_TYPE_POLICY_FAILURE, "Sampling Window"),
                            getConfiguredValueFromSingleContextVariable(assertion.getPolicyFailureCircuitMaxFailures(), variableMap, CIRCUIT_TYPE_POLICY_FAILURE, "Max Failures"))
            );
        }
        return null;
    }

    private CircuitConfig getLatencyCircuitConfig(final Map<String, Object> variableMap) {
        if (assertion.isLatencyCircuitEnabled()) {
            return new CircuitConfig(getConfiguredValue(assertion.isLatencyCircuitCustomTrackerIdEnabled() ? assertion.getLatencyCircuitTrackerId() : latencyCircuitTrackerId, variableMap, CIRCUIT_TYPE_LATENCY),
                    getConfiguredValueFromSingleContextVariable(assertion.getLatencyCircuitRecoveryPeriod(), variableMap, CIRCUIT_TYPE_LATENCY, "Recovery Period"),
                    new Latency(getConfiguredValueFromSingleContextVariable(assertion.getLatencyCircuitSamplingWindow(), variableMap, CIRCUIT_TYPE_LATENCY, "Sampling Window"),
                            getConfiguredValueFromSingleContextVariable(assertion.getLatencyCircuitMaxFailures(), variableMap, CIRCUIT_TYPE_LATENCY, "Max Failures"),
                            getConfiguredValueFromSingleContextVariable(assertion.getLatencyCircuitMaxLatency(), variableMap, CIRCUIT_TYPE_LATENCY, "Max Latency")));
        }
        return null;
    }

    private boolean isCircuitClosed(final CircuitConfig circuitConfig, final boolean usesCustomTracker) {
        if (null == circuitConfig) {
            return true;
        }

        long now = timeSource.currentTimeMillis();
        Long circuitCloseTimestamp = circuitStateManager.getState(circuitConfig);
        if (circuitCloseTimestamp != null && circuitCloseTimestamp > now) {
            Date circuitCloseDate = new Date(circuitCloseTimestamp);
            String detailString = new SimpleDateFormat("HH:mm:ss:SSS zzz").format(circuitCloseDate);

            if (usesCustomTracker) {
                detailString += " for Event Tracker ID '" + circuitConfig.getTrackerId() + "'";
            }

            logAndAudit(AssertionMessages.CB_CIRCUIT_OPEN, circuitConfig.getFailureCondition().getType(), detailString);
            return false;
        }
        return true;
    }

    private EventTracker getEventTracker(final String trackerId) {
        EventTracker eventTracker = eventTrackerManager.getEventTracker(trackerId);
        if (null == eventTracker) {
            eventTracker = eventTrackerManager.createEventTracker(trackerId);
        }
        return eventTracker;
    }

    private void auditCircuitTripped(final CircuitConfig circuitConfig) {
        Date circuitCloseDate = new Date(circuitStateManager.getState(circuitConfig));
        final String timeString = new SimpleDateFormat("HH:mm:ss:SSS zzz").format(circuitCloseDate);
        logAndAudit(AssertionMessages.CB_CIRCUIT_TRIPPED, circuitConfig.getFailureCondition().getType(), timeString);
    }

    private void logLatencyLimitExceededEvent(final String trackerId, long currentExecutionLatency) {
        Calendar latencyOccurrenceTime = Calendar.getInstance();
        final String timeString = new SimpleDateFormat("HH:mm:ss:SSS zzz").format(latencyOccurrenceTime.getTime());
        logger.log(Level.INFO, "Max Latency exceeded for Circuit with Event Tracker ID '" + trackerId + "' at " + timeString +
                ". Policy execution latency was " + TimeUnit.NANOSECONDS.toMillis(currentExecutionLatency) + " ms.");
    }

    private boolean isAnyCircuitForcedOpen(CircuitConfig policyFailureCircuit, CircuitConfig latencyCircuit) {
        if (null != policyFailureCircuit && eventTrackerManager.isCircuitForcedOpen(policyFailureCircuit.getTrackerId())) {
            logAndAudit(AssertionMessages.CB_FORCED_CIRCUIT_TRIPPED, CIRCUIT_TYPE_POLICY_FAILURE, policyFailureCircuit.getTrackerId());
            return true;
        } else if (null != latencyCircuit && eventTrackerManager.isCircuitForcedOpen(latencyCircuit.getTrackerId())) {
            logAndAudit(AssertionMessages.CB_FORCED_CIRCUIT_TRIPPED, CIRCUIT_TYPE_LATENCY, latencyCircuit.getTrackerId());
            return true;
        }
        return false;
    }

    private int getConfiguredValueFromSingleContextVariable(final String variable, final Map<String, Object> variableMap, final String circuitType, final String field) {
        String stringVariable = null;
        boolean isInvalid;
        int intValue = 0;
        try {
            if (Syntax.isAnyVariableReferenced(variable)) {
                stringVariable = String.valueOf(ExpandVariables.processSingleVariableAsObject(variable, variableMap, getAudit()));
            } else {
                stringVariable = variable;
            }
            intValue = Integer.parseInt(stringVariable);
            isInvalid = intValue < 1;
        } catch (NumberFormatException e) {
            isInvalid = true;
        }
        if (isInvalid) {
            logAndAudit(AssertionMessages.CB_CIRCUIT_INVALID_CONFIGURATION, circuitType, field, stringVariable);
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        } else {
            return intValue;
        }
    }

    private String getConfiguredValue(final String variable, final Map<String, Object> variableMap, final String circuitType) {
        String stringVariable = null;
        if (StringUtils.isNotEmpty(variable)) {
            if (Syntax.isAnyVariableReferenced(variable)) {
                stringVariable = String.valueOf(ExpandVariables.process(variable, variableMap, getAudit()));
            } else {
                stringVariable = variable;
            }
        }
        if (StringUtils.isEmpty(stringVariable) || StringUtils.isEmpty(variable)) {
            logAndAudit(AssertionMessages.CB_CIRCUIT_INVALID_CONFIGURATION, circuitType, "Custom Event Tracker ID", "null/empty");
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }
        return stringVariable;
    }
}
