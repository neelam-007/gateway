package com.l7tech.server.message.metrics;

import com.l7tech.server.message.PolicyEnforcementContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class containing methods to set the {@link GatewayMetricsPublisher} and to pass the publisher from the parent PEC to kid PEC
 */
public final class GatewayMetricsUtils {

    /**
     * Set the {@link GatewayMetricsPublisher} if the PEC supports the {@link GatewayMetricsSupport}.
     * Set to null if the publisher has no subscribers.
     *
     * @param context the context of which the publisher is set to. Cannot be {@code null}.
     * @param publisher the publisher to be set to. The publisher will be {@code null} in the case there are no subscribers {@link GatewayMetricsListener}
     */
    public static void setPublisher(@NotNull final PolicyEnforcementContext context, @Nullable final GatewayMetricsPublisher publisher) {
        if (context instanceof GatewayMetricsSupport) {
            ((GatewayMetricsSupport) context).setGatewayMetricsEventsPublisher(publisher != null && publisher.hasSubscribers() ? publisher : null);
        }
    }

    /**
     * passes the publisher from the parent PEC to the kid PEC if the PEC supports the {@link GatewayMetricsSupport}.
     *
     * @param parent the parent PEC containing the publisher {@link GatewayMetricsPublisher}. Cannot be {@code null}
     * @param child the kid PEC to pass the publisher to {@link GatewayMetricsPublisher}. Cannot be {@code null}.
     */
    public static void setPublisher(@NotNull final PolicyEnforcementContext parent, @NotNull final PolicyEnforcementContext child) {
        // pass GatewayMetricsPublisher from parent to child PEC
        if (parent instanceof GatewayMetricsSupport && child instanceof GatewayMetricsSupport) {
            ((GatewayMetricsSupport) child).setGatewayMetricsEventsPublisher(((GatewayMetricsSupport) parent).getGatewayMetricsEventsPublisher());
        }
    }
}
