/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

/**
 * TransportMetadata for testing.
 */
public class TestTransportMetadata extends TransportMetadata {
    public TransportProtocol getProtocol() {
        return TransportProtocol.UNKNOWN;
    }

    public Object getParameter(String name) {
        return null;
    }
}
