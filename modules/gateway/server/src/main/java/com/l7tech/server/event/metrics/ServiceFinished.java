package com.l7tech.server.event.metrics;

import com.l7tech.server.message.metrics.LatencyMetrics;
import org.jetbrains.annotations.NotNull;

/**
 * Event the {@link com.l7tech.server.message.metrics.GatewayMetricsPublisher GatewayMetricsPublisher} publishes when a service is finished.
 */
public interface ServiceFinished extends GatewayMetricsEvent {
    /**
     * @return the associated {@link LatencyMetrics}, never {@code null}.
     */
    @NotNull
    LatencyMetrics getServiceMetrics();
}
