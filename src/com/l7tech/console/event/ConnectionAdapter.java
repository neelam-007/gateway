package com.l7tech.console.event;

/**
 * The abstract adapter which receives connection events. The methods
 * in this class are empty; this class is provided as a convenience
 * for easily creating listeners by extending this class and overriding
 * only the methods of interest.

 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class ConnectionAdapter implements ConnectionListener {
    /**
     * Invoked on connection event
     * @param e describing the connection event
     */
    public void onConnect(ConnectionEvent e) {
    }

    /**
     * Invoked on disconnect
     * @param e describing the dosconnect event
     */
    public void onDisconnect(ConnectionEvent e) {
    }
}
