package com.l7tech.console.event;

import java.util.EventListener;

/**
 * This is the Listener interface for Connection events.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface ConnectionListener  extends EventListener {
    /**
     * Invoked on connection event
     * @param e describing the connection event
     */
    void onConnect(ConnectionEvent e);

    /**
     * Invoked on disconnect
     * @param e describing the dosconnect event
     */
    void onDisconnect(ConnectionEvent e);

}
