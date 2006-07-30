/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.audit;

import com.l7tech.common.security.rbac.Permission;
import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.Set;

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
    private final Set<Permission> permissions;

    /**
     * create the connection event
     *
     * @param source the event source
     * @param type   the event type
     */
    public LogonEvent(Object source, int type) {
        super(source);
        this.type = type;
        this.permissions = Collections.emptySet();
    }

    /**
     * create the connection event
     *
     * @param source the event source
     * @param type   the event type
     * @param permissions the set of {@link Permission}s granted to the logged-on user
     */
    public LogonEvent(Object source, int type, Set<Permission> permissions) {
        super(source);
        this.type = type;
        if (type == LOGON)
            this.permissions = Collections.unmodifiableSet(permissions);
        else
            this.permissions = Collections.emptySet();
    }

    /**
     * @return the event type
     */
    public int getType() {
        return type;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}