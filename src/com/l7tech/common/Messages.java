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
    private static Map messagesById = new HashMap();

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

    public String getSeverityLevelNameById(int id) {
        M message = (M) messagesById.get(new Integer(id));
        if(message != null) return message.getLevelName();
        return null;
    }
}
