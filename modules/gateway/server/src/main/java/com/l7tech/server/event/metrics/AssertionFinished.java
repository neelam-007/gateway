package com.l7tech.server.event.metrics;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.message.metrics.LatencyMetrics;
import org.jetbrains.annotations.NotNull;

/**
 * Event the {@link com.l7tech.server.message.metrics.GatewayMetricsPublisher GatewayMetricsPublisher} publishes when an assertion is finished.
 */
public interface AssertionFinished extends GatewayMetricsEvent {
    /**
     * @return the associated {@link LatencyMetrics}, never {@code null}.
     */
    @NotNull
    LatencyMetrics getAssertionMetrics();

    /**
     * @return the assertion that finished executing, never {@code null}.
     */
    @NotNull
    Assertion getAssertion();
}
