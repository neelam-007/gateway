/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.event.system;

import com.l7tech.common.Component;
import org.springframework.context.ApplicationEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

/**
 * @author alex
 */
public abstract class SystemEvent extends ApplicationEvent {
    private static final String MY_IP = getIp();

    private static String getIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "<unknown>";
        }
    }

    public SystemEvent(Object source, Component component) {
        this(source, component, null, Level.INFO);
    }

    public SystemEvent(Object source, Component component, String ipAddress, Level level) {
        this(source, component, ipAddress, level, null);
    }

    public SystemEvent(Object source, Component component, String ipAddress, Level level, String message) {
        super(source);
        this.component = component;
        this.level = level;
        if (ipAddress == null) ipAddress = MY_IP;
        this.ipAddress = ipAddress;
        this.message = message != null ? message : component.getName() + " " + getAction();
    }

    public abstract String getAction();

    public String getMessage() {
        return message;
    }

    public Component getComponent() {
        return component;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Level getLevel() {
        return level;
    }

    private final Component component;
    private final String ipAddress;
    private final Level level;
    private final String message;
}
