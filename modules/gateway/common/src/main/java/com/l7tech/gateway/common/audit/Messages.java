/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * Superclass for message catalogs, and holds generic exception messages.
 * The ID range 0001-0999 inclusive is reserved for these messages.
 *
 * @author alex
 */
public class Messages {
    /**
     * Reserved IDs:
     *
     * Messages:                       0001 - 0099  (Generic messages, e.g. Exception)
     * CommonMessages:                 0100 - 0999
     * BootMessages:                   1000 - 1499
     * AdminMessages:                  1500 - 1999
     * SystemMessages:                 2000 - 2999
     * MessageProcessingMessages:      3000 - 3499
     * ServiceMessages:                3500 - 3999
     * AssertionMessagess:             4000 - 99999
     * EsmMessages:                    100000 - 109999
     *
     */

    /**
     * this is the prefix for cluster propreties which override default message values as you would
     * normally get through Messages.getMessageById(id). the prefix is followed by the id of the message
     * which is to be overriden
     */
    public static final String OVERRIDE_PREFIX = "auditmsg.override.";

    private static final AtomicBoolean allowMessageRegistration = new AtomicBoolean(true);
    private static final Map<Integer, AuditDetailMessage> messagesById = new HashMap<Integer, AuditDetailMessage>();
    private static final ReadWriteLock messageLock = new ReentrantReadWriteLock(false);

    // must appear after the instantiation of messageById HaspMap.
    // NOTE: *_SEVERE is the same as *_WARNING since you should never log at SEVERE anything except "audits flushed"
    // and other events that must be recorded indelibly FOREVER
    public static final M EXCEPTION_SEVERE                  = m(1, Level.WARNING, "Exception caught! ");
    public static final M EXCEPTION_SEVERE_WITH_MORE_INFO   = m(2, Level.WARNING, "{0}.  Exception caught! ");
    public static final M EXCEPTION_WARNING                 = m(3, Level.WARNING, "Exception caught! ");
    public static final M EXCEPTION_WARNING_WITH_MORE_INFO  = m(4, Level.WARNING, "{0}.  Exception caught! ");
    public static final M EXCEPTION_INFO                    = m(5, Level.INFO, "Exception caught! ");
    public static final M EXCEPTION_INFO_WITH_MORE_INFO     = m(6, Level.INFO, "{0}.  Exception caught! ");
    // MAX -                                                  m(0099)

    static AuditDetailMessage getAuditDetailMessageById(int id) {
        messageLock.readLock().lock();
        try {
            return Messages.messagesById.get(id);
        } finally {
            messageLock.readLock().unlock();
        }
    }

    static void lockMessages() {
        allowMessageRegistration.set( false );
    }

    protected Messages() { }

    protected static M m(int id, Level level, String msg) {
        return m(id, level, false, false, msg);
    }

    protected static M m(int id, Level level, boolean saveRequest, boolean saveResponse, String msg) {
        if ( !allowMessageRegistration.get() ) throw new IllegalStateException("Audit messages are locked");

        M adm = new M(id, level, msg, saveRequest, saveResponse);
        messageLock.writeLock().lock();
        Object o;
        try {
            o = messagesById.put(id, adm);
        } finally {
            messageLock.writeLock().unlock();
        }
        if (o != null) throw new IllegalArgumentException("A message with id #" + id + " already exists!");
        return adm;
    }

    public static final class M extends AuditDetailMessage {
        public M(int id, Level level, String message, boolean saveRequest, boolean saveResponse) {
            super(id, level, message, saveRequest, saveResponse);
        }

        public M(int id, Level level, String message) {
            this(id, level, message, false, false);
        }
    }
}