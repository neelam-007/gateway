package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion.BLACKOUT_PERIOD;
import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion.FAILURE_THRESHOLD;

/**
 * Server side implementation of the CircuitBreakerAssertion.
 *
 * @see com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion
 */
public class ServerCircuitBreakerAssertion extends ServerCompositeAssertion<CircuitBreakerAssertion> {

    private static final Logger logger = Logger.getLogger(ServerCircuitBreakerAssertion.class.getName());

    private static Counter counter = new Counter();
    private static final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private static final AtomicLong circuitCloseTime = new AtomicLong(0);

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

    void setCounter(Counter counter) {
        ServerCircuitBreakerAssertion.counter = counter;
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        // check if the circuit is open and if so, has the blackout period been exceeded yet
        if (circuitOpen.get()) {
            if (circuitCloseTime.get() > System.currentTimeMillis()) {
                final Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(circuitCloseTime.get());
                final String timeString = new SimpleDateFormat("HH:mm:ss:SSS").format(cal.getTime());
                logger.log(Level.WARNING, "Circuit closed until " + timeString);
                return AssertionStatus.FALSIFIED;
            } else {
                circuitOpen.set(false);
            }
        }

        // use execution time to record failures to account for long operations or timeouts
        long executionTimestamp = System.currentTimeMillis();

        // run child policy
        AssertionStatus status = iterateChildren(context, assertionResultListener);

        handleChildExecutionResult(executionTimestamp, status);

        return status;
    }

    private void handleChildExecutionResult(long executionTimestamp, AssertionStatus status) {
        // check if failure needs to be recorded
        if (AssertionStatus.NONE != status) {
            counter.recordFailure(executionTimestamp);

            // open circuit if failure threshold has been exceeded
            if (counter.getCountSinceTimestamp(0L) >= FAILURE_THRESHOLD) {
                circuitOpen.set(true);
                circuitCloseTime.set(System.currentTimeMillis() + BLACKOUT_PERIOD);
                counter.reset();
            }
        }
    }
}
