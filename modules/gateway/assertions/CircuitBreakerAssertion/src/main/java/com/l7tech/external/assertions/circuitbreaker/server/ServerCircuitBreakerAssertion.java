package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;
import com.l7tech.util.TimeSource;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the CircuitBreakerAssertion.
 *
 * @see com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion
 */
public class ServerCircuitBreakerAssertion extends ServerCompositeAssertion<CircuitBreakerAssertion> {

    private static final Logger logger = Logger.getLogger(ServerCircuitBreakerAssertion.class.getName());
    private final String policyFailureTrackerId;
    private final String latencyFailureTrackerId;

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
        if(null != assertion.getPolicyFailureTrackerId()) {
            policyFailureTrackerId = assertion.getPolicyFailureTrackerId();
        } else {
            policyFailureTrackerId = "policy_" + String.valueOf(Math.abs(secureRandom.nextLong()));
        }

        if(null != assertion.getLatencyFailureTrackerId()) {
            latencyFailureTrackerId = assertion.getLatencyFailureTrackerId();
        } else {
            latencyFailureTrackerId = "latency_" + String.valueOf(Math.abs(secureRandom.nextLong()));
        }
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // check if the circuit is open and if so, has the recovery period been exceeded yet
        CircuitConfig policyFailureCircuit = getFailureCircuitConfig();
        CircuitConfig latencyFailureCircuit = getLatencyCircuitConfig();

        if (!isCircuitClosed(policyFailureCircuit) ||  !isCircuitClosed(latencyFailureCircuit)) {
            return AssertionStatus.FALSIFIED;
        }

        long executionStartTimestamp = timeSource.nanoTime();
        // run child policy
        final AssertionStatus status = iterateChildren(context, assertionResultListener);
        long executionEndTimestamp = timeSource.nanoTime();
        long currentExecutionLatency = executionEndTimestamp - executionStartTimestamp;

        //handle policy failures and latency failures
        if (null != policyFailureCircuit  && AssertionStatus.NONE != status) {
            updateEventTracker(policyFailureCircuit, executionEndTimestamp);
        }
        if (null != latencyFailureCircuit && currentExecutionLatency > TimeUnit.MILLISECONDS.toNanos(((LatencyFailure)latencyFailureCircuit.getFailureCondition()).getLimit())) {
            updateEventTracker(latencyFailureCircuit, executionEndTimestamp);
        }

       return status;
    }

   private void updateEventTracker(final CircuitConfig circuitConfig, final long timeStamp) {
        EventTracker eventTracker = getEventTracker(circuitConfig.getTrackerId());
        eventTracker.recordEvent(timeStamp);

        long sinceTimeStamp = timeStamp - TimeUnit.MILLISECONDS.toNanos(
                circuitConfig.getFailureCondition().getSamplingWindow());
        long count = eventTracker.getCountSinceTimestamp(sinceTimeStamp);
        logger.log(Level.FINE, String.format("Event count is %s. Max count is #%s ", count,
                circuitConfig.getFailureCondition().getMaxFailureCount()));

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

    private CircuitConfig getFailureCircuitConfig() {
        if (assertion.isPolicyFailureEnabled()) {
            return new CircuitConfig(policyFailureTrackerId,
                    assertion.getPolicyFailureRecoveryPeriod(),
                    new PolicyFailure(assertion.getPolicyFailureSamplingWindow(), assertion.getPolicyFailureMaxCount()));

        }
        return null;
    }

    private CircuitConfig getLatencyCircuitConfig() {
        if (assertion.isLatencyFailureEnabled()){
            return new CircuitConfig(latencyFailureTrackerId,
                    assertion.getLatencyRecoveryPeriod(),
                    new LatencyFailure(assertion.getLatencyFailureSamplingWindow(),
                            assertion.getLatencyFailureMaxCount(), assertion.getLatencyFailureLimit()));
        }
        return null;
    }

    private boolean isCircuitClosed(final CircuitConfig circuitConfig) {
        if (null == circuitConfig) {
            return true;
        }

        long now = timeSource.currentTimeMillis();
        Long circuitCloseTimestamp = circuitStateManager.getState(circuitConfig);
        if (circuitCloseTimestamp != null && circuitCloseTimestamp > now) {
            logCircuitOpenStatus(circuitConfig, circuitCloseTimestamp);
            return false;
        }
        return true;
    }

    private void logCircuitOpenStatus(final CircuitConfig circuitConfig, final long circuitCloseTimestamp) {
        Date circuitCloseDate = new Date(circuitCloseTimestamp);
        final String timeString = new SimpleDateFormat("HH:mm:ss:SSS zzz").format(circuitCloseDate);
        logger.log(Level.WARNING, "Circuit open until " + timeString + " for trackerID=" + circuitConfig.getTrackerId());
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
}
