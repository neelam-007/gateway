package com.l7tech.console.event;

import java.util.EventObject;

/**
 * This class represents the connection events.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ConnectionEvent extends EventObject {
    public static int CONNECTED = 0;
    public static int DISCONNECTED = 1;


    private final int type;

    /**
     * create the connection event
     * 
     * @param source the event source
     * @param type the event type
     */
    public ConnectionEvent(Object source, int type) {
        super(source);
        this.type = type;
    }

    /**
     * @return the event type
     */
    public int getType() {
        return type;
    }

}
