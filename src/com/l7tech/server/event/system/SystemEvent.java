/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.system;

import com.l7tech.common.Component;
import com.l7tech.server.event.Event;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class SystemEvent extends Event {
    public SystemEvent(Object source, Component component) {
        this(source, component, null);
    }

    public SystemEvent(Object source, Component component, String ipAddress) {
        super(source);
        this.component = component;

        try {
            if (ipAddress == null) ipAddress = InetAddress.getLocalHost().getHostAddress();
            this.ipAddress = ipAddress;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }

    public abstract String getAction();

    public Component getComponent() {
        return component;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    private final Component component;
    private final String ipAddress;
}
