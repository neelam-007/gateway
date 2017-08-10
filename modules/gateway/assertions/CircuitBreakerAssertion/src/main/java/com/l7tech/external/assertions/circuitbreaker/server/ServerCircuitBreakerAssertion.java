package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;
import com.l7tech.util.TimeSource;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion.*;

/**
 * Server side implementation of the CircuitBreakerAssertion.
 *
 * @see com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion
 */
public class ServerCircuitBreakerAssertion extends ServerCompositeAssertion<CircuitBreakerAssertion> {

    private static TimeSource timeSource = new TimeSource();

    private static final Logger logger = Logger.getLogger(ServerCircuitBreakerAssertion.class.getName());
    public static final String POLICY_FAILURE_CONDITION_NAME = "Policy Failure";
    public static final String LATENCY_FAILURE_CONDITION_NAME = "Latency Failure";

    private Counter counter = new Counter();
    private Counter latencyCounter = new Counter();
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final AtomicLong circuitCloseTime = new AtomicLong(0);
    private final AtomicBoolean latencyCircuitOpen = new AtomicBoolean(false);
    private final AtomicLong latencyCircuitCloseTime = new AtomicLong(0);

    private final String[] variablesUsed;

    private final AssertionResultListener assertionResultListener = (context, result) -> {
        if (result != AssertionStatus.NONE) {
            seenAssertionStatus(context, result);
            rollbackDeferredAssertions(context);
            return false;
        }
        return true;
    };

    public ServerCircuitBreakerAssertion(final CircuitBreakerAssertion assertion, ApplicationContext applicationContext) throws PolicyAssertionException, LicenseException {
        super(assertion, applicationContext);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        long circuitEntryTimestamp = timeSource.nanoTime();

        // check if the either policy failure or latency failure circuit is open and if so, has the recovery period been exceeded yet
        if (!isCircuitClosed(circuitOpen, circuitCloseTime, circuitEntryTimestamp, POLICY_FAILURE_CONDITION_NAME) ||
                !isCircuitClosed(latencyCircuitOpen, latencyCircuitCloseTime, circuitEntryTimestamp, LATENCY_FAILURE_CONDITION_NAME)) {
            return AssertionStatus.FALSIFIED;
        }

        long executionStartTimestamp = timeSource.nanoTime();
        // run child policy
        final AssertionStatus status = iterateChildren(context, assertionResultListener);

        long executionEndTimestamp = timeSource.nanoTime();

        handlePolicyExecutionResult(executionEndTimestamp, status);
        handlePolicyExecutionLatency(executionStartTimestamp, executionEndTimestamp);

        return status;
    }

    private boolean isCircuitClosed(AtomicBoolean circuitOpen, AtomicLong circuitCloseTime, long executionTimestamp, String name) {
        if (circuitOpen.get()) {
            if (circuitCloseTime.get() > executionTimestamp) {
                Date circuitCloseDate = new Date(TimeUnit.NANOSECONDS.toMillis(circuitCloseTime.get()));
                final String timeString = new SimpleDateFormat("HH:mm:ss:SSS zzz").format(circuitCloseDate);
                logger.log(Level.INFO, name + " Circuit open until " + timeString);
                return false;
            } else {
                circuitOpen.set(false);

                logAndAudit(AssertionMessages.CB_CIRCUIT_CLOSED, name);
            }
        }
        return true;
    }

    private void handlePolicyExecutionResult(final long executionTimestamp, final AssertionStatus status) {
        // check if failure needs to be recorded
        if (AssertionStatus.NONE != status) {
            updateCounter(POLICY_FAILURE_CONDITION_NAME, executionTimestamp, FAILURE_THRESHOLD, RECOVERY_PERIOD, counter, circuitOpen, circuitCloseTime);
        }
    }

    private void handlePolicyExecutionLatency(final long executionStartTimestamp, long executionEndTimestamp) {
        // check if failure / latency needs to be recorded
        long currentExecutionLatency = executionEndTimestamp - executionStartTimestamp;

        if(currentExecutionLatency > TimeUnit.MILLISECONDS.toNanos(LATENCY_TIME_THRESHOLD)) {
            updateCounter(LATENCY_FAILURE_CONDITION_NAME, executionEndTimestamp, LATENCY_THRESHOLD, LATENCY_RECOVERY_PERIOD, latencyCounter, latencyCircuitOpen, latencyCircuitCloseTime);
        }
    }

    private synchronized void updateCounter(final String policyType, final long executionEndTimestamp, final long threshold, final long recoveryPeriod, final Counter counter, final AtomicBoolean circuitOpen, final AtomicLong circuitCloseTime) {
        counter.recordFailure(executionEndTimestamp);

        // open circuit if policy / latency failure threshold has been exceeded
        if (counter.getCountSinceTimestamp(0L) >= threshold) {
            circuitOpen.set(true);
            circuitCloseTime.set(timeSource.nanoTime() + TimeUnit.MILLISECONDS.toNanos(recoveryPeriod));
            counter.reset();

            auditCircuitTripped(policyType, circuitCloseTime);
        }
    }

    private void auditCircuitTripped(String failureType, final AtomicLong circuitCloseTime) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(TimeUnit.NANOSECONDS.toMillis(circuitCloseTime.get()));
        final String timeString = new SimpleDateFormat("HH:mm:ss:SSS").format(cal.getTime());

        logAndAudit(AssertionMessages.CB_CIRCUIT_TRIPPED, failureType, timeString);
    }

    /**
     *
     * This method is used for setting TestTimeSource during unit testing.
     *
     * */
    static void setTimeSource(TimeSource timeSource) {
        ServerCircuitBreakerAssertion.timeSource = timeSource;
    }
}
