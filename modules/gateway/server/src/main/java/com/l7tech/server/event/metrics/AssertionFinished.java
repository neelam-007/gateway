package com.l7tech.server.event.metrics;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetrics;
import org.jetbrains.annotations.NotNull;

/**
 * Event the {@link com.l7tech.server.message.metrics.GatewayMetricsPublisher GatewayMetricsPublisher} publishes when an assertion is finished.
 */
public interface AssertionFinished extends GatewayMetricsEvent {
    /**
     * @return the associated {@link AssertionMetrics}, never {@code null}.
     */
    @NotNull
    AssertionMetrics getAssertionMetrics();

    /**
     * @return the assertion that finished executing, never {@code null}.
     */
    @NotNull
    Assertion getAssertion();
}
