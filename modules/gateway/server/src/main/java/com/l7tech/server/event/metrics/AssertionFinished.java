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
        return assertionMetrics;
    }

    @NotNull
    public Assertion getAssertion() {
        return assertion;
    }
}
