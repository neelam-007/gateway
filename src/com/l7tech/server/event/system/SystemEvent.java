/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.system;

import com.l7tech.common.Component;
import org.springframework.context.ApplicationEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class SystemEvent extends ApplicationEvent {
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
