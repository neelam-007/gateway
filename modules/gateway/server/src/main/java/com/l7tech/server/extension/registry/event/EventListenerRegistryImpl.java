package com.l7tech.server.extension.registry.event;

import com.ca.apim.gateway.extension.event.Event;
import com.ca.apim.gateway.extension.event.EventListener;
import com.l7tech.server.extension.registry.AbstractRegistryImpl;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import static java.util.Collections.unmodifiableCollection;

/**
 * Registry for {@link EventListener} extensions.
 */
public class EventListenerRegistryImpl extends AbstractRegistryImpl<EventListener> implements EventListenerRegistry {

    private static final Logger LOGGER = Logger.getLogger(EventListenerRegistryImpl.class.getName());

    @Override
    public <E extends Event> Collection<EventListener<E>> getEventListenersFor(Class<E> eventType) {
        Collection<EventListener<E>> listeners = new ArrayList<>();
        for (EventListener listener : this.getTaggedExtensions(eventType.getName())) {
            listeners.add(listener);
        }
        return unmodifiableCollection(listeners);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void register(String key, EventListener extension, String... tags) {
        // Force tagging with the event type to easily recover when triggered
        super.register(key, extension, ArrayUtils.add(tags, extension.supportedEventType().getName()));
    }

}
