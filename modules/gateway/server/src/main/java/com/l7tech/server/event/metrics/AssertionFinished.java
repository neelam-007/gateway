package com.l7tech.server.event.metrics;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetrics;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.jetbrains.annotations.NotNull;

/**
 * Event the {@link com.l7tech.server.message.metrics.GatewayMetricsPublisher GatewayMetricsPublisher} publishes when an assertion is finished.
 */
public final class AssertionFinished extends GatewayMetricsEvent {
    private final Assertion assertion;
    private final AssertionMetrics assertionMetrics;

    /**
     * Constructor
     * @param context the context of the current running assertion
     * @param assertion the finished assertion
     * @param assertionMetrics the metrics containing the assertion latency
     */
    public AssertionFinished(
            @NotNull final PolicyEnforcementContext context,
            @NotNull final Assertion assertion,
            @NotNull final AssertionMetrics assertionMetrics
    ) {
        super(context);
        this.assertion = assertion;
        this.assertionMetrics = assertionMetrics;
    }

    /**
     * @return the associated {@link AssertionMetrics}, never {@code null}.
     */
    @NotNull
    public AssertionMetrics getAssertionMetrics() {
        // AssertionMetrics is immutable class therefore it can easily be passed to different thread.
        // Uncomment the line below if that changes in the future (in which case don't forget to update the appropriate unit test and docs)
        // checkOwnerThread();
        return assertionMetrics;
    }

    /**
     * @return the assertion that finished executing, never {@code null}.
     */
    @NotNull
    public Assertion getAssertion() {
        // Not sure whether Assertion object is OK to be passed to different thread
        // comment out the line below if it is FINE (in which case don't forget to update the appropriate unit test and docs)
        checkOwnerThread();
        return assertion;
    }
}
