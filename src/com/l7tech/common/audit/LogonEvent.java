/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.audit;

import org.springframework.context.ApplicationEvent;

/**
 * This class represents the logon events.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class LogonEvent extends ApplicationEvent {
    public static final int LOGON = 0x0;
    public static final int LOGOFF = 0x1;


    private final int type;

    /**
     * create the connection event
     *
     * @param source the event source
     * @param type   the event type
     */
    public LogonEvent(Object source, int type) {
        super(source);
        this.type = type;
    }

    /**
     * @return the event type
     */
    public int getType() {
        return type;
    }
}