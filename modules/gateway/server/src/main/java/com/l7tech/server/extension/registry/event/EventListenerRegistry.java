package com.l7tech.server.extension.registry.event;

import com.ca.apim.gateway.extension.ExtensionRegistry;
import com.ca.apim.gateway.extension.event.Event;
import com.ca.apim.gateway.extension.event.EventListener;

import java.util.Collection;

/**
 * Defines a type of extension registry that holds {@link EventListener}s.
 */
public interface EventListenerRegistry extends ExtensionRegistry<EventListener> {

    /**
     * Get the event listeners for a specific type of event.
     *
     * @param eventType Type of the event to lookup for listeners
     * @return List of {@link EventListener}s, empty if no listener available.
     */
    <E extends Event> Collection<EventListener<E>> getEventListenersFor(Class<E> eventType);
}
