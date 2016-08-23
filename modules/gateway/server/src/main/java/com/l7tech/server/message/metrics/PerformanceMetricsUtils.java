package com.l7tech.server.message.metrics;

import com.l7tech.server.message.PolicyEnforcementContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TODO: add some javadoc
 */
public final class PerformanceMetricsUtils {

    public static void setPublisher(@NotNull final PolicyEnforcementContext context, @Nullable final PerformanceMetricsPublisher publisher) {
        if (context instanceof PerformanceMetricsSupport) {
            ((PerformanceMetricsSupport) context).setPerformanceMetricsEventsPublisher(publisher != null && publisher.hasSubscribers() ? publisher : null);
        }
    }

    public static void setPublisher(@NotNull final PolicyEnforcementContext parent, @NotNull final PolicyEnforcementContext child) {
        // pass PerformanceMetricsPublisher from parent to child PEC
        if (parent instanceof PerformanceMetricsSupport && child instanceof PerformanceMetricsSupport) {
            ((PerformanceMetricsSupport) child).setPerformanceMetricsEventsPublisher(((PerformanceMetricsSupport) parent).getPerformanceMetricsEventsPublisher());
        }
    }
}
