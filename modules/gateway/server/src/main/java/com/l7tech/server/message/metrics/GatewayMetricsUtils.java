package com.l7tech.server.message.metrics;

import com.l7tech.server.message.PolicyEnforcementContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TODO: add some javadoc
 */
public final class GatewayMetricsUtils {

    public static void setPublisher(@NotNull final PolicyEnforcementContext context, @Nullable final GatewayMetricsPublisher publisher) {
        if (context instanceof GatewayMetricsSupport) {
            ((GatewayMetricsSupport) context).setGatewayMetricsEventsPublisher(publisher != null && publisher.hasSubscribers() ? publisher : null);
        }
    }

    public static void setPublisher(@NotNull final PolicyEnforcementContext parent, @NotNull final PolicyEnforcementContext child) {
        // pass GatewayMetricsPublisher from parent to child PEC
        if (parent instanceof GatewayMetricsSupport && child instanceof GatewayMetricsSupport) {
            ((GatewayMetricsSupport) child).setGatewayMetricsEventsPublisher(((GatewayMetricsSupport) parent).getGatewayMetricsEventsPublisher());
        }
    }
}
