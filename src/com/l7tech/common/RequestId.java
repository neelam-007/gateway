/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common;

/**
 * Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public class RequestId {
    public RequestId( int serverId, long bootTime, long sequence ) {
        _serverId = serverId;
        _bootTime = bootTime;
        _sequence = sequence;
    }

    public int getServerId() {
        return _serverId;
    }

    public long getBootTime() {
        return _bootTime;
    }

    public long getSequence() {
        return _sequence;
    }

    private int _serverId;
    private long _bootTime;
    private long _sequence;
}
