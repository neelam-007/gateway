/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.objectmodel.event.Event;

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

    public static void fire(Event event) {
        List listeners = new ArrayList();
        collectListeners(listeners, event.getClass());
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
            listenersByType.put( eventClass, listener );
            write.release();
            write = null;
        } catch ( InterruptedException e ) {
            logger.log( Level.INFO, "Interrupted while waiting for write lock", e );
            Thread.currentThread().interrupt();
        } finally {
            if (write != null) write.release();
        }
    }

    private static void collectListeners(List listeners, Class eventClass) {
        if (listeners == null) throw new IllegalArgumentException("listeners must not be null");
        if (eventClass == null) throw new IllegalArgumentException("eventClass must not be null");
        if (!eventClass.isAssignableFrom(EventObject.class))
            throw new IllegalArgumentException(eventClass.getName() + " is not assignable from EventObject");

        Sync read = lock.readLock();
        try {
            read.acquire();
            List temp = (List)listenersByType.get(eventClass);
            if (temp != null) listeners.addAll(temp);
            Class sup = eventClass.getSuperclass();
            if (sup != null) collectListeners(listeners, sup);
            read.release();
            read = null;
        } catch ( InterruptedException e ) {
            logger.log( Level.WARNING, "Interrupted while waiting for read lock", e );
            Thread.currentThread().interrupt();
        } finally {
            if (read != null) read.release();
        }
    }

    private static ReadWriteLock lock = new ReaderPreferenceReadWriteLock();
    private static Map listenersByType = new HashMap();

    private static Logger logger = Logger.getLogger(EventManager.class.getName());
}
