package com.l7tech.server.message.metrics;

import com.l7tech.server.event.metrics.AssertionFinished;
import org.jetbrains.annotations.NotNull;

/**
 * TODO: override methods to receive individual events if interested
 */
public abstract class PerformanceMetricsListener {

    @SuppressWarnings("UnusedParameters")
    public void assertionFinished(@NotNull final AssertionFinished event) {
        // nothing to do in the base
    }

    /**
     * TODO: javadoc why we are overriding this and not allowing the implementer to override
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * TODO: javadoc why we are overriding this and not allowing the implementer to override
     */
    @Override
    public final boolean equals(final Object obj) {
        return super.equals(obj);
    }
}
