/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

/**
 * Information about a message that arrived over TCP. This could be an inbound request (front side) or an inbound
 * response (back side)
 */
public interface TcpKnob {
    String getRemoteAddress();
    String getRemoteHost();
    int getRemotePort();
    int getLocalPort();
}
