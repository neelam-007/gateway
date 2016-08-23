package com.l7tech.server.message.metrics;

import org.jetbrains.annotations.Nullable;

/**
 * TODO: add javadoc
 */
public interface PerformanceMetricsSupport {
    void setPerformanceMetricsEventsPublisher(@Nullable PerformanceMetricsPublisher publisher);
    @Nullable PerformanceMetricsPublisher getPerformanceMetricsEventsPublisher();
}
