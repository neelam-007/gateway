/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import net.sf.hibernate.Transaction;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Manages the registration of {@link EventListener}s and {@link EventPromoter}s, and manages the propagation of
 * {@link Event}s to them according to the type of event.
 * @author alex
 * @version $Revision$
 */
public class EventManager {
    private EventManager() { }

    public static void removeListener(EventListener listener) {
        removeFromMapOfSets(listener, listenersByEventClass);
    }

    public static void removePromoter(EventPromoter promoter) {
        removeFromMapOfSets(promoter, promotersByEventClass);
    }

    public static void fireInNewTransaction(Event event) throws TransactionException {
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext) HibernatePersistenceContext.getCurrent();
            Transaction t = context.getAuditSession().beginTransaction();
            fire(event);
            t.commit();
        } catch ( Exception e ) {
            throw new TransactionException("Unable to commit event transaction", e);
        }
    }

    public static void fire(Event event) {
        logger.fine("Firing event " + event);
        Set listeners = new HashSet();
        collectEventClassMap(listenersByEventClass, listeners, event.getClass());
        Set promoters = new HashSet();
        collectEventClassMap(promotersByEventClass, promoters, event.getClass());
        for ( Iterator i = promoters.iterator(); i.hasNext(); ) {
            EventPromoter promoter = (EventPromoter) i.next();
            Event old = event;
            event = promoter.promote(event);
            if (event != old) {
                logger.finer(old + " was promoted to " + event);
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
        addToMapOfSets(eventClass, listener, listenersByEventClass);
    }

    public static void addPromoter(Class eventClass, EventPromoter promoter) {
        if (eventClass == null || promoter == null) throw new IllegalArgumentException("Both parameters must be non-null");
        if (!Event.class.isAssignableFrom(eventClass)) throw new IllegalArgumentException(eventClass.getName() + " is not derived from " + Event.class.getName());
        if (promoter.getClass() == EventPromoter.class) throw new IllegalArgumentException("promoter class must be derived from " + EventPromoter.class.getName() + ", not equal to it");
        addToMapOfSets(eventClass, promoter, promotersByEventClass);
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

    private static void addToMapOfSets(Class eventClass, Object thing, Map map) {
        Sync write = lock.writeLock();
        try {
            write.acquire();
            Set things = (Set)map.get(eventClass);
            if (things == null) {
                things = new HashSet();
                map.put(eventClass, things);
            }
            things.add(thing);
        } catch (InterruptedException e ) {
            logger.warning("Interrupted waiting for lock");
            Thread.currentThread().interrupt();
        } finally {
            if (write != null) write.release();
        }
    }

    private static void removeFromMapOfSets(Object thing, Map map) {
        Sync read = lock.readLock();
        Sync write = lock.writeLock();
        try {
            read.acquire();
            for ( Iterator i = map.keySet().iterator(); i.hasNext(); ) {
                Class eventClass = (Class)i.next();
                Set listeners = (Set)map.get(eventClass);
                if (listeners.contains(thing)) {
                    read.release();
                    write.acquire();
                    listeners.remove(thing);
                    write.release();
                    read.acquire();
                }
            }
        } catch ( InterruptedException e ) {
            logger.log( Level.INFO, "Interrupted waiting for lock", e );
            Thread.currentThread().interrupt();
        } finally {
            if (write != null) write.release();
            if (read != null) read.release();
        }
    }

    private static ReadWriteLock lock = new ReaderPreferenceReadWriteLock();
    private static Map listenersByEventClass = new HashMap();
    private static Map promotersByEventClass = new HashMap();

    private static Logger logger = Logger.getLogger(EventManager.class.getName());
}
