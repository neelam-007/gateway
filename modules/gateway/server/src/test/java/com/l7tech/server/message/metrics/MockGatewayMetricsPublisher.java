package com.l7tech.server.message.metrics;

import com.l7tech.server.event.metrics.AssertionFinished;
import com.l7tech.server.event.metrics.ServiceFinished;
import org.jetbrains.annotations.NotNull;

/**
 * Simple subclass of {@link GatewayMetricsPublisher} that overrides the
 * {@link GatewayMetricsPublisher#publishEvent(AssertionFinished) publishEvent()} method to make it public
 * so unit test code can verify that calls were made to it.
 */
public class MockGatewayMetricsPublisher extends GatewayMetricsPublisher {

    @Override
    public void publishEvent(@NotNull final ServiceFinished event) {
        super.publishEvent(event);
    }
}
