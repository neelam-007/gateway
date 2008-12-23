package com.l7tech.server.ems.listener;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when the HTTP listener configuration is successfully used.
 */
public class ListenerConfigurationUpdatedEvent extends ApplicationEvent {

    //- PUBLIC

    /**
     * Create a configuration update event with the given info.
     */
    public ListenerConfigurationUpdatedEvent( final Object source,
                                              final String ipAddress,
                                              final int port,
                                              final String alias ) {
        super( source );
        this.ipAddress = ipAddress;
        this.port = port;
        this.alias = alias;
    }

    /**
     * Get the alias.
     *
     * @return The alias that was used.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Get the IP address.
     *
     * @return The IP address that was used.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Get the port address.
     *
     * @return The port that was used.
     */
    public int getPort() {
        return port;
    }

    //- PRIVATE

    private String ipAddress;
    private int port;
    private String alias;
}
