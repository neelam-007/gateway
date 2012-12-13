/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;
import org.jetbrains.annotations.Nullable;
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

    public SystemEvent(Object source, Component component, @Nullable String ipAddress, Level level) {
        this(source, component, ipAddress, level, null, -1, null, null);
    }

    public SystemEvent(Object source, Component component, @Nullable String ipAddress, Level level, String message) {
        this(source, component, ipAddress, level, message, -1, null, null);
    }

    public SystemEvent(
            Object source,
            Component component,
            @Nullable String ipAddress,
            Level level,
            String message,
            long identityProviderOid,
            String userName,
            String userId) {
        super(source);
        this.component = component;
        this.level = level;
        if (ipAddress == null) ipAddress = MY_IP;
        this.ipAddress = ipAddress;
        this.message = message;
        this.identityProviderOid = identityProviderOid;
        this.userName = userName;
        this.userId = userId;
    }

    public abstract String getAction();

    public String getMessage() {
        return message != null ? message : component.getName() + " " + getAction();
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

    public long getIdentityProviderOid() {
        return identityProviderOid;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserId() {
        return userId;
    }

    private final Component component;
    private final String ipAddress;
    private final Level level;
    private final String message;
    private final long identityProviderOid;
    private final String userName;
    private final String userId;
}
