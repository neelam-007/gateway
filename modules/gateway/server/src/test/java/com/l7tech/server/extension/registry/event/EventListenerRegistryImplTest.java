package com.l7tech.server.extension.registry.event;

import com.ca.apim.gateway.extension.event.Event;
import com.ca.apim.gateway.extension.event.EventListener;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link EventListenerRegistryImpl}
 */
public class EventListenerRegistryImplTest {

    private EventListenerRegistryImpl registry = new EventListenerRegistryImpl();

    @Before
    public void before() {
        registry.register("event1", new EventListenerTest1());
        registry.register("event2", new EventListenerTest2());
    }

    @Test
    public void ensureListenersTaggedWithEventClassName() {
        Collection<EventListener> extensions = registry.getTaggedExtensions(EventTest1.class.getName());
        assertNotNull(extensions);
        assertTrue(!extensions.isEmpty());
        assertEquals(EventListenerTest1.class, extensions.iterator().next().getClass());

        extensions = registry.getTaggedExtensions(EventTest2.class.getName());
        assertNotNull(extensions);
        assertTrue(!extensions.isEmpty());
        assertEquals(EventListenerTest2.class, extensions.iterator().next().getClass());
    }

    @Test
    public void testRecoverListenerByEventClass() {
        Collection<EventListener<EventTest1>> listeners1 = this.registry.getEventListenersFor(EventTest1.class);
        assertNotNull(listeners1);
        assertTrue(!listeners1.isEmpty());
        assertEquals(EventListenerTest1.class, listeners1.iterator().next().getClass());

        Collection<EventListener<EventTest2>> listeners2 = this.registry.getEventListenersFor(EventTest2.class);
        assertNotNull(listeners2);
        assertTrue(!listeners2.isEmpty());
        assertEquals(EventListenerTest2.class, listeners2.iterator().next().getClass());

        Collection<EventListener<EventTest3>> listeners3 = this.registry.getEventListenersFor(EventTest3.class);
        assertNotNull(listeners3);
        assertTrue(listeners3.isEmpty());
    }

    private static class EventTest1 extends ApplicationEvent implements Event {

        public EventTest1(Object source) {
            super(source);
        }
    }

    private static class EventTest2 extends ApplicationEvent implements Event {

        public EventTest2(Object source) {
            super(source);
        }
    }

    private static class EventTest3 extends ApplicationEvent implements Event {

        public EventTest3(Object source) {
            super(source);
        }
    }

    private static class EventListenerTest1 implements EventListener<EventTest1> {

        @Override
        public void onEvent(EventTest1 event) {
        }

        @Override
        public Class<EventTest1> supportedEventType() {
            return EventTest1.class;
        }
    }

    private static class EventListenerTest2 implements EventListener<EventTest2> {

        @Override
        public void onEvent(EventTest2 event) {
        }

        @Override
        public Class<EventTest2> supportedEventType() {
            return EventTest2.class;
        }
    }
}
