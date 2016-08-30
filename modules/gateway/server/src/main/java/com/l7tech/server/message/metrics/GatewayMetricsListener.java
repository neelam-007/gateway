package com.l7tech.server.message.metrics;

import com.l7tech.server.event.metrics.AssertionFinished;
import org.jetbrains.annotations.NotNull;

/**
 * If a developer wants to create a new listener to subscribe to GatewayMetric events.
 * They must override the individual methods they are interested in receiving.
 * Otherwise, leave the implementations blank.
 */
public abstract class GatewayMetricsListener {

    /**
     * @param event contains the context, assertion, and assertionMetrics {@link AssertionFinished}, cannot be {@code null}
     */
    @SuppressWarnings("UnusedParameters")
    public void assertionFinished(@NotNull final AssertionFinished event) {
        // nothing to do in the base
    }

    /**
     * This class might be extended by various developers,
     * preventing overrides ensures that all listeners will have the same hashcode() and equals() implementation.
     * Prevent implementers from overriding this method and therefore prevent any collision
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * This class might be extended by various developers,
     * preventing overrides ensures that all listeners will have the same hashcode() and equals() implementation.
     * Implementers of this class is not allowed to override the way listeners are differentiated from each other
     */
    @Override
    public final boolean equals(final Object obj) {
        return super.equals(obj);
    }
}
