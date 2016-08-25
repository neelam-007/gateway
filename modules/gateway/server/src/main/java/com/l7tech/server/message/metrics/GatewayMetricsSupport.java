package com.l7tech.server.message.metrics;

import org.jetbrains.annotations.Nullable;

/**
 * TODO: add javadoc
 */
public interface GatewayMetricsSupport {
    void setGatewayMetricsEventsPublisher(@Nullable GatewayMetricsPublisher publisher);
    @Nullable
    GatewayMetricsPublisher getGatewayMetricsEventsPublisher();
}
