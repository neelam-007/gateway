package com.l7tech.server.transport.http;

import org.apache.http.conn.ClientConnectionManager;

/**
 * Listener for HttpConnectionManagerListener events.
 */
public interface HttpConnectionManagerListener {

    /**
     * Notify the listener of HttpConnectionManager instance creation.
     *
     * @param manager The ClientConnectionManager (should not be null)
     */
    void notifyHttpConnectionManagerCreated( ClientConnectionManager manager );

    /**
     * Notify the listener of HttpConnectionManager instance destroy.
     *
     * @param manager The ClientConnectionManager (should not be null)
     */
    void notifyHttpConnectionManagerDestroyed( ClientConnectionManager manager );


    /**
     * Adapter for listener implementations.
     */
    class HttpConnectionManagerListenerAdapter implements HttpConnectionManagerListener {
        @Override
        public void notifyHttpConnectionManagerCreated( final ClientConnectionManager manager ) {
        }

        @Override
        public void notifyHttpConnectionManagerDestroyed(ClientConnectionManager manager) {
        }
    }
}
