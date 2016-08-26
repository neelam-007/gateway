package com.l7tech.server.event.metrics;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetrics;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.jetbrains.annotations.NotNull;

/**
 * TODO: add javadoc
 */
public final class AssertionFinished extends GatewayMetricsEvent {
    private final Assertion assertion;
    private final AssertionMetrics assertionMetrics;

    public AssertionFinished(
            @NotNull final PolicyEnforcementContext context,
            @NotNull final Assertion assertion,
            @NotNull final AssertionMetrics assertionMetrics
    ) {
        super(context);
        this.assertion = assertion;
        this.assertionMetrics = assertionMetrics;
    }

    @NotNull
    public AssertionMetrics getAssertionMetrics() {
        // AssertionMetrics is immutable class therefore it can easely be passed to different thread.
        // Uncomment the line below if that changes in the future (in which case don't forget to update the appropriate unit test)
        // checkOwnerThread();
        return assertionMetrics;
    }

    @NotNull
    public Assertion getAssertion() {
        // Not sure whether Assertion object is OK to be passed to different thread
        // comment out the line below if it is FINE (in which case don't forget to update the appropriate unit test)
        checkOwnerThread();
        return assertion;
    }
}
