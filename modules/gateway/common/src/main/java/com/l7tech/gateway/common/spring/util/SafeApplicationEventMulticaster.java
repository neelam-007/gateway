package com.l7tech.gateway.common.spring.util;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ApplicationEventMulticaster multicaster implementation that supports runtime listener changes.
 *
 * <p>Just add an instance of this class as a Spring bean and off you go.</p>
 *
 * <p>It is safe to add and remove listeners at any time.</p>
 *
 * <p>Exceptions in listeners are caught and logged.</p>
 *
 * TODO see if we can remove this now the spring multicaster supports registration
 */
public class SafeApplicationEventMulticaster implements ApplicationEventMulticaster {

    //- PUBLIC

    public SafeApplicationEventMulticaster() {
        listeners = new CopyOnWriteArraySet();
    }

    public void addApplicationListener(ApplicationListener listener) {
        if(listener==null) throw new IllegalArgumentException("listener must not be null.");
        listeners.add(listener);
    }

    @Override
    public void addApplicationListenerBean( final String s ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeApplicationListenerBean( final String s ) {
        throw new UnsupportedOperationException();
    }

    public void multicastEvent(ApplicationEvent event) {
        if(event==null) throw new IllegalArgumentException("event must not be null.");
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            ApplicationListener applicationListener = (ApplicationListener) iterator.next();
            try {
                applicationListener.onApplicationEvent(event);
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected exception during event dispatch.", e);
            }
        }
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    public void removeApplicationListener(ApplicationListener listener) {
        if(listener==null) throw new IllegalArgumentException("listener must not be null.");
        listeners.remove(listener);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SafeApplicationEventMulticaster.class.getName());

    private final Set listeners;

}
