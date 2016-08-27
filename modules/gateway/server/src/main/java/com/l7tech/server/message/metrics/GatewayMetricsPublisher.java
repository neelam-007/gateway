package com.l7tech.server.message.metrics;

import com.l7tech.server.event.metrics.AssertionFinished;
import com.l7tech.server.policy.module.AssertionModuleUnregistrationEvent;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ExceptionUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO: add javadoc
 */
public final class GatewayMetricsPublisher implements PostStartupApplicationListener {
    private static final Logger logger = Logger.getLogger(GatewayMetricsPublisher.class.getName());

    // don't care about the order in which an event will be published to subscribers
    private final Set<GatewayMetricsListener> subscribers = new ConcurrentHashSet<>();

    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        if (event instanceof AssertionModuleUnregistrationEvent) {
            final ClassLoader classLoader = ((AssertionModuleUnregistrationEvent)event).getModuleClassLoader();
            final List<GatewayMetricsListener> toRemove = new ArrayList<>();
            for (final GatewayMetricsListener subscriber : subscribers) {
                if (subscriber.getClass().getClassLoader() == classLoader) {
                    toRemove.add(subscriber);
                }
            }
            subscribers.removeAll(toRemove);
        }
    }

    public void addListener(@NotNull final GatewayMetricsListener listener) {
        subscribers.add(listener);
    }

    public void removeListener(@NotNull final GatewayMetricsListener listener) {
        subscribers.remove(listener);
    }

    @SuppressWarnings("WeakerAccess")
    public boolean hasSubscribers() {
        return !subscribers.isEmpty();
    }

    public void publishEvent(@NotNull final AssertionFinished event) {
        for (final GatewayMetricsListener subscriber : subscribers) {
            try {
                subscriber.assertionFinished(event);
            } catch (final Throwable ex) {
                logger.log(
                        Level.WARNING,
                        "Error while publishing AssertionFinished event for subscriber \"" + subscriber.getClass().getName() + "\":" + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex)
                );
            }
        }
    }
}
