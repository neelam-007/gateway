package com.l7tech.server.transport.jms2;

import com.l7tech.server.transport.jms.JmsRuntimeException;

import java.util.concurrent.atomic.AtomicLong;

public interface CachedConnection {
    JmsSessionHolder borrowConnection() throws JmsRuntimeException;

    void returnConnection(JmsSessionHolder connection) throws JmsRuntimeException;

    JmsEndpointConfig getEndpointConfig();

    void close();

    long getCreatedTime();

    void touch();

    AtomicLong getLastAccessTime();

    void invalidate(JmsSessionHolder connection) throws Exception;

    boolean isDisconnected();

    boolean isActive();

    boolean isIdleTimeoutExpired();

    void debugStatus();

    @Override
    String toString();
}
