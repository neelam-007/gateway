/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.objectmodel.event.Event;
import com.l7tech.objectmodel.event.PersistenceEvent;
import com.l7tech.server.service.ServiceEventPromoter;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author alex
 * @version $Revision$
 */
public class EventManager {
    private EventManager() { }

    public static void removeListener(EventListener eventListener) {
        Sync read = lock.readLock();
        Sync write = lock.writeLock();
        try {
            read.acquire();
            for ( Iterator i = listenersByEventClass.keySet().iterator(); i.hasNext(); ) {
                Class eventClass = (Class)i.next();
                Set listeners = (Set)listenersByEventClass.get(eventClass);
                if (listeners.contains(eventListener)) {
                    read.release();
                    write.acquire();
                    listeners.remove(eventListener);
                    write.release();
                    read.acquire();
                }
            }
        } catch ( InterruptedException e ) {
            logger.log( Level.INFO, "Interrupted waiting for write lock", e );
            Thread.currentThread().interrupt();
        } finally {
            if (write != null) write.release();
            if (read != null) read.release();
        }
    }

    private static Map promotersByEventClass = new HashMap();

    static {
        HashSet promoters = new HashSet();
        promoters.add(new ServiceEventPromoter());
        promotersByEventClass.put(PersistenceEvent.class, promoters);
    }

    public static void fire(Event event) {
        logger.info("Firing event " + event);
        Set listeners = new HashSet();
        collectEventClassMap(listenersByEventClass, listeners, event.getClass());
        Set promoters = new HashSet();
        collectEventClassMap(promotersByEventClass, promoters, event.getClass());
        for ( Iterator i = promoters.iterator(); i.hasNext(); ) {
            EventPromoter promoter = (EventPromoter) i.next();
            Event old = event;
            event = promoter.promote(event);
            if (event != old) {
                logger.info(old + " was promoted to " + event); // TODO finer
            }
        }

        for ( Iterator i = listeners.iterator(); i.hasNext(); ) {
            EventListener listener = (EventListener)i.next();
            event.sendTo(listener);
        }
    }

    public static void addListener(Class eventClass, EventListener listener) {
        if (eventClass == null || listener == null) throw new IllegalArgumentException("Both parameters must be non-null");
        if (!Event.class.isAssignableFrom(eventClass)) throw new IllegalArgumentException(eventClass.getName() + " is not derived from " + Event.class.getName());
        if (listener.getClass() == EventListener.class) throw new IllegalArgumentException("listener class must be derived from " + EventListener.class.getName() + ", not equal to it");
        Sync write = lock.writeLock();
        try {
            write.acquire();
            Set listeners = (Set)listenersByEventClass.get(eventClass);
            if (listeners == null) {
                listeners = new HashSet();
                listenersByEventClass.put(eventClass, listeners);
            }
            listeners.add(listener);
            write.release();
            write = null;
        } catch ( InterruptedException e ) {
            logger.log( Level.INFO, "Interrupted while waiting for write lock", e );
            Thread.currentThread().interrupt();
        } finally {
            if (write != null) write.release();
        }
    }

    private static void collectEventClassMap(Map map, Set listeners, Class eventClass) {
        if (listeners == null) throw new IllegalArgumentException("listeners must not be null");
        if (eventClass == null) throw new IllegalArgumentException("eventClass must not be null");
        if (!EventObject.class.isAssignableFrom(eventClass))
            throw new IllegalArgumentException(eventClass.getName() + " is not assignable from EventObject");

        Sync read = lock.readLock();
        try {
            read.acquire();
            Set temp = (Set)map.get(eventClass);
            if (temp != null) listeners.addAll(temp);
            Class sup = eventClass.getSuperclass();
            if (sup == Object.class) {
                return;
            } else {
                collectEventClassMap(map, listeners, sup);
            }
        } catch ( InterruptedException e ) {
            logger.log( Level.WARNING, "Interrupted while waiting for read lock", e );
            Thread.currentThread().interrupt();
        } finally {
            if (read != null) read.release();
        }
    }

    private static ReadWriteLock lock = new ReaderPreferenceReadWriteLock();
    private static Map listenersByEventClass = new HashMap();

    private static Logger logger = Logger.getLogger(EventManager.class.getName());
}
