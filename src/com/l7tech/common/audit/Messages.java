/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.common.audit;

import java.util.HashMap;
import java.util.Map;
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
     * Messages:                       0001 - 0999  (Generic messages, e.g. Exception)
     * BootMessages:                   1000 - 1999
     * SystemMessages:                 2000 - 2999
     * MessageProcessingMessages:      3000 - 3999
     * AssertionMessagess:             4000 - 7999
     *
     *
     */

    private static final Map messagesById = new HashMap();

    // must appear after the instantiation of messageById HaspMap.
    // NOTE: *_SEVERE is the same as *_WARNING since you should never log at SEVERE anything except "audits flushed"
    // and other events that must be recorded indelibly FOREVER
    public static final M EXCEPTION_SEVERE                  = m(1, Level.WARNING, "Exception caught! ");
    public static final M EXCEPTION_SEVERE_WITH_MORE_INFO   = m(2, Level.WARNING, "{0}. Exception caught! ");
    public static final M EXCEPTION_WARNING                 = m(3, Level.WARNING, "Exception caught! ");
    public static final M EXCEPTION_WARNING_WITH_MORE_INFO  = m(4, Level.WARNING, "{0}. Exception caught! ");
    public static final M EXCEPTION_INFO                    = m(5, Level.INFO, "Exception caught! ");
    public static final M EXCEPTION_INFO_WITH_MORE_INFO     = m(6, Level.INFO, "{0}. Exception caught! ");
    // MAX -                                                  m(0999)

    private static final Class[] SUBCLASSES = {
        // Make sure these always get loaded, so the static intializers run
        AssertionMessages.class,
        BootMessages.class,
        MessageProcessingMessages.class,
        SystemMessages.class
    };

    protected Messages() { }

    protected static M m(int id, Level level, String msg) {
        M adm = new M(id, level, msg);
        Object o = messagesById.put(new Integer(id), adm);
        if (o != null) throw new IllegalArgumentException("A message with id #" + id + " already exists!");
        return (M)adm;
    }

    public static final class M extends AuditDetailMessage {
        public M(int id, Level level, String message) {
            super(id, level, message);
        }
    }

    public static String getMessageById(int id) {
        M message = (M) messagesById.get(new Integer(id));
        if(message != null) return message.getMessage();
        return null;
    }

    public static Level getSeverityLevelById(int id) {
        M message = (M) messagesById.get(new Integer(id));
        if(message != null) return message.getLevel();
        return null;
    }

    public static String getSeverityLevelNameById(int id) {
        M message = (M) messagesById.get(new Integer(id));
        if(message != null) return message.getLevel().getName();
        return null;
    }
}