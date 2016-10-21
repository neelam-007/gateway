package com.l7tech.server.message.metrics;

import com.l7tech.server.event.metrics.AssertionFinished;
import com.l7tech.server.event.metrics.ServiceFinished;
import com.l7tech.server.policy.module.AssertionModuleUnregistrationEvent;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dedicated GatewayMetrics event publisher with a set containing the subscribers.
 * Developers can subscribe/unsubscribe themselves from the publisher. Events are published only if subscribers exist.
 * Subscribers are automatically unsubscribed when the monitoring assertion is unloaded.
 */
public final class GatewayMetricsPublisher implements PostStartupApplicationListener {
    private static final Logger logger = Logger.getLogger(GatewayMetricsPublisher.class.getName());

    // don't care about the order in which an event will be published to subscribers
    // TODO: change to ConcurrentHashMap.newKeySet() once we up the language level, as the method is available from 1.8 onwards
    private final Set<GatewayMetricsListener> subscribers = Collections.newSetFromMap(new ConcurrentHashMap<GatewayMetricsListener, Boolean>());

    /**
     * Handle {@link AssertionModuleUnregistrationEvent} in order to remove all subscribers associated to an assertion
     * once its module is unloaded.
     */
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

    /**
     * add subscriber to the publisher
     * @param listener the listener {@link GatewayMetricsListener} to subscribe to publisher. Cannot be {@code null}.
     */
    public void addListener(@NotNull final GatewayMetricsListener listener) {
        subscribers.add(listener);
    }

    /**
     * remove the subscriber from the publisher
     * @param listener the listener to remove {@link GatewayMetricsListener}. Cannot be {@code null}.
     */
    public void removeListener(@NotNull final GatewayMetricsListener listener) {
        subscribers.remove(listener);
    }

    /**
     * check if the publisher has subscribers {@link GatewayMetricsListener}
     * @return true if the set of subscribers is not empty. {@code false} if the set is empty.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean hasSubscribers() {
        return !subscribers.isEmpty();
    }

    /**
     * publishes the {@link AssertionFinished} event to each {@link GatewayMetricsListener subscriber}.
     *
     * @param event the event {@link AssertionFinished} to be published. Cannot be {@code null}
     */
    void publishEvent(@NotNull final AssertionFinished event) {
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

    /**
     * publishes the {@link ServiceFinished} event to each {@link GatewayMetricsListener subscriber}.
     *
     * @param event the event {@link ServiceFinished} to be published. Cannot be {@code null}
     */
    void publishEvent(@NotNull final ServiceFinished event) {
        for (final GatewayMetricsListener subscriber : subscribers) {
            try {
                subscriber.serviceFinished(event);
            } catch (final Throwable ex) {
                logger.log(
                        Level.WARNING,
                        "Error while publishing ServiceFinished event for subscriber \"" + subscriber.getClass().getName() + "\":" + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex)
                );
            }
        }
    }
}
