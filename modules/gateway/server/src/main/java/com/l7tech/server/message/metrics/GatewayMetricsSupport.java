package com.l7tech.server.message.metrics;

import org.jetbrains.annotations.Nullable;

/**
 * an interface that the PEC should extend off of in order to support the {@link GatewayMetricsPublisher}
 */
public interface GatewayMetricsSupport {
    /**
     * sets the {@link GatewayMetricsPublisher}
     *
     * @param publisher the publisher to be set. Can be {@code null} in the case there are no subscribers {@link GatewayMetricsListener}
     */
    void setGatewayMetricsEventsPublisher(@Nullable GatewayMetricsPublisher publisher);

    /**
     * gets the {@link GatewayMetricsPublisher}
     *
     * @return the GatewayMetricsPublisher or {@code null} if it does not exist
     */
    @Nullable
    GatewayMetricsPublisher getGatewayMetricsEventsPublisher();
}
