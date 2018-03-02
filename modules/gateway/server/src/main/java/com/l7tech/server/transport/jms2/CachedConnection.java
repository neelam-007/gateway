package com.l7tech.server.transport.jms2;

import com.l7tech.server.transport.jms.JmsRuntimeException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * interface CachedConnection
 */
public interface CachedConnection {
    /**
     * borrows SessionHolder from CachedConnection object
     * @return SessionHolder object. Can represent single session or pool of sessions
     * @throws JmsRuntimeException when unable to borrow
     * @throws JmsConnectionMaxWaitException when MaxWait expires
     */
    SessionHolder borrowConnection() throws JmsRuntimeException, JmsConnectionMaxWaitException;

    /**
     * returns SessionHolder object to the CachedConnection
     * @param connection - SessionHolder
     * @throws JmsRuntimeException when unable to return connection
     */
    void returnConnection(SessionHolder connection) throws JmsRuntimeException;

    /**
     * gets JmsEndpointConfig object from CachedConnection
     * @return
     */
    JmsEndpointConfig getEndpointConfig();

    /**
     * closes open CachedConnection and destroys all SessionHolder objects
     */
    void close();

    /**
     * return a long value when the object was created
     * @return
     */
    long getCreatedTime();

    /**
     * modifies timeouts
     */
    void touch();

    /**
     * return last time when the CachedConnection object was accessed (touch() called)
     * @return
     */
    AtomicLong getLastAccessTime();

    /**
     * invalidates SessionHolder
     * @param connection SessionHolder
     * @throws Exception if method failed
     */
    void invalidate(SessionHolder connection) throws Exception;

    /**
     * @return true if disconnected
     */
    boolean isDisconnected();

    /**
     * @return true if CachedConnection object is active
     */
    boolean isActive();

    /**
     * @return true if idle timeout expired
     */
    boolean isIdleTimeoutExpired();

    /**
     * displays debug status
     */
    void debugStatus();

    @Override
    String toString();
}
