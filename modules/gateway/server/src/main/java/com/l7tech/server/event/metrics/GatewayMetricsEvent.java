package com.l7tech.server.event.metrics;

import com.l7tech.server.message.PolicyEnforcementContext;
import org.jetbrains.annotations.NotNull;

/**
 * Tag interface for any future Gateway Metrics related Events.
 */
public interface GatewayMetricsEvent {
    @NotNull
    PolicyEnforcementContext getContext();
}
