/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common;

import com.l7tech.common.audit.AuditDetailMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class Messages {
    /**
     * Reserved IDs:
     *
     * Messages:                  0001 - 0999  (Generic messages, e.g. Exception)
     * BootMessages:              1000 - 1999
     * AuditMessages:             2000 - 2999
     * MessageProcessingMessages: 3000 - 3999
     * AssertionMessagess:        4000 - 4999
     *
     *
     */

    private static Map messagesById = new HashMap();

    // must appear after the instantiation of messageById HaspMap.
    public static final M EXCEPTION_SEVERE                  = m(1, Level.SEVERE, "Exception caught: ");
    public static final M EXCEPTION_WARNING                 = m(2, Level.WARNING, "Exception caught: ");

    protected Messages() { }

    protected static final M m(int id, Level level, String msg) {
        M adm = new M(id, level, msg);
        Object o = messagesById.put(new Integer(id), adm);
        if (o != null) throw new IllegalArgumentException("A message with id #" + id + " already exists!");
        return (M)adm;
    }

    protected static final class M extends AuditDetailMessage {
        public M(int id, Level level, String message) {
            super(id, level, message);
        }
    }

    public String getMessageById(int id) {
        M message = (M) messagesById.get(new Integer(id));
        if(message != null) return message.getMessage();
        return null;
    }

    public Level getSeverityLevelById(int id) {
        M message = (M) messagesById.get(new Integer(id));
        if(message != null) return message.getLevel();
        return null;
    }
}
