package com.l7tech.server.util;

import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import static org.junit.Assert.*;
import org.junit.*;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ApplicationEventProxyTest {
    private List<Pair<ApplicationEvent, ApplicationListener>> deliveries = new ArrayList<Pair<ApplicationEvent, ApplicationListener>>();

    private final Listener la = new Listener();
    private final Listener lb = new Listener();
    private final Event e1 = new Event();
    private final Event e2 = new Event();

    private ApplicationEventProxy proxy;

    private class Listener implements ApplicationListener {
        public void onApplicationEvent(ApplicationEvent event) {
            deliveries.add(new Pair<ApplicationEvent, ApplicationListener>(event, this));
        }
    }

    private class Event extends ApplicationEvent {
        private Event() {
            super(new Object());
        }

        public Event(Object source) {
            super(source);
        }
    }

    private void delivered(int num) {
        assertEquals("Should have performed " + num + " event deliveries total", num, deliveries.size());
    }

    // Delivery #index shall have delivered this event to this listener
    private void delivery(int index, Event event, ApplicationListener listener) {
        assertEquals(new Pair<ApplicationEvent, ApplicationListener>(event, listener), deliveries.get(index));
    }

    @Before
    public void setUp() {
        deliveries.clear();
        proxy = new ApplicationEventProxy();
    }
    
    @Test
    public void testProxying() {
        proxy.addApplicationListener(la);
        proxy.addApplicationListener(lb);
        proxy.onApplicationEvent(e1);
        proxy.onApplicationEvent(e2);
        delivered(4);
        delivery(0, e1, la);
        delivery(1, e1, lb);
        delivery(2, e2, la);
        delivery(3, e2, lb);
    }
    
    @Test
    public void testManualUnsubscribe() {
        proxy.addApplicationListener(la);
        proxy.addApplicationListener(lb);
        proxy.onApplicationEvent(e1);
        proxy.removeApplicationListener(la);
        proxy.onApplicationEvent(e2);
        delivered(3);
        delivery(0, e1, la);
        delivery(1, e1, lb);
        delivery(2, e2, lb);
    }

    @Test(expected=NullPointerException.class)
    public void testNullListener() {
        proxy.addApplicationListener(null);
    }

    @Test
    public void testDestroy() throws Exception {
        proxy.addApplicationListener(la);
        proxy.addApplicationListener(lb);
        proxy.destroy();
        proxy.onApplicationEvent(e1);
        proxy.onApplicationEvent(e2);
        delivered(0);
    }

    public static class TestListener implements org.springframework.context.ApplicationListener {
        private final Functions.UnaryVoid<ApplicationEvent> callback;

        public TestListener(Functions.UnaryVoid<ApplicationEvent> callback) {
            this.callback = callback;
        }

        public void onApplicationEvent(ApplicationEvent event) {
            callback.call(event);
        }
    }

    @Test
    public void testRemoveListenersFromClassLoader() throws Exception {
        final String classname = TestListener.class.getName();
        ClassLoader cl = new ClassLoader() {
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (!classname.equals(name))
                    return super.loadClass(name);
                try {
                    byte[] bytes = IOUtils.slurpUrl(TestListener.class.getClassLoader().getResource(classname.replace('.', '/') + ".class"));
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        final ApplicationListener[] listenerHolder = { null };
        Class clazz = cl.loadClass(classname);
        listenerHolder[0] = (ApplicationListener)clazz.getConstructor(Functions.UnaryVoid.class).newInstance(new Functions.UnaryVoid<ApplicationEvent>() {
            public void call(ApplicationEvent event) {
                deliveries.add(new Pair<ApplicationEvent, ApplicationListener>(event, listenerHolder[0]));
            }
        });
        final ApplicationListener lt = listenerHolder[0];

        proxy.addApplicationListener(lt);
        proxy.addApplicationListener(la);
        proxy.onApplicationEvent(e1);
        proxy.removeListenersFromClassLoader(lt.getClass().getClassLoader());
        assertEquals(1, proxy.subscribers.size());
        proxy.onApplicationEvent(e2);
        delivered(3);
        delivery(0, e1, lt);
        delivery(1, e1, la);
        delivery(2, e2, la);
    }  
}

