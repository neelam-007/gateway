package com.l7tech.server.log;

/**
 * Interface that can be implemented by Handlers that wish to recieve notification
 * on server logging started. 
 */
public interface StartupAwareHandler {
    void notifyStarted();
}
