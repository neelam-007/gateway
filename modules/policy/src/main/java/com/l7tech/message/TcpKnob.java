/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

/**
 * Information about a message that arrived over TCP. This could be an inbound request (front side) or an inbound
 * response (back side)
 */
@SuppressWarnings({"UnusedDeclaration"}) // Suppressed because apparently-unused methods are invoked reflectively by MessageSelector
public interface TcpKnob extends MessageKnob {
    /**
     * @return  remote IPv4 or IPv6 address in conventional (dotted or colon) notation, or null if not known.
     */
    String getRemoteAddress();

    /**
     * @return the remote hostname, or else IP address, or else null.
     */
    String getRemoteHost();

    /**
     * @return the remote TCP port, or 0 if not known.
     */
    int getRemotePort();

    /**
     * @return the local side of the connection's IPv4 or IPv6 address in conventional notation, or null if not known.
     */
    String getLocalAddress();

    /**
     * @return the local side of the connection's hostname, or else IP address, or else null.
     */
    String getLocalHost();

    /**
     * @return the local TCP port, or 0 if not known.
     */
    int getLocalPort();
}
