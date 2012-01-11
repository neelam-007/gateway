package com.l7tech.server.transport.http;

import org.apache.commons.httpclient.HttpConnectionManager;

/**
 * Listener for HttpConnectionManagerListener events.
 */
public interface HttpConnectionManagerListener {

    /**
     * Notify the listener of HttpConnectionManager instance creation.
     *
     * @param manager The HttpConnectionManager (should not be null)
     */
    void notifyHttpConnectionManagerCreated( HttpConnectionManager manager );

    /**
     * Adapter for listener implementations.
     */
    class HttpConnectionManagerListenerAdapter implements HttpConnectionManagerListener {
        @Override
        public void notifyHttpConnectionManagerCreated( final HttpConnectionManager manager ) {
        }
    }
}
