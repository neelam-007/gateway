/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.RequestId;

/**
 * Generates probably-unique request identifiers incorporating a server ID,
 * (see {@link ServerConfig#getServerId()}) a boot time and sequence number.
 *
 * @author alex
 * @version $Revision$
 */
public class RequestIdGenerator {
    public static RequestId next() {
        RequestIdGenerator instance = getInstance();
        long seq;
        long time;
        synchronized( instance ) {
            seq = instance.nextSequence();
            time = instance._bootTime;
        }
        return new RequestId(time, seq);
    }

    private static synchronized RequestIdGenerator getInstance() {
        if ( _instance == null ) _instance = new RequestIdGenerator();
        return _instance;
    }

    private void reseed( long time ) {
        _bootTime = time;
    }

    private RequestIdGenerator() {
        ServerConfig config = ServerConfig.getInstance();
        _serverId = config.getServerId();
        reseed( config.getServerBootTime() );
    }

    private long nextSequence() {
        if ( _sequence >= MAX_SEQ ) {
            _sequence = 0;
            reseed( System.currentTimeMillis() );
        }
        return _sequence++;
    }

    private long _bootTime;
    private int _serverId;
    private long _sequence;

    private static RequestIdGenerator _instance;
    public static final long MAX_SEQ = Long.MAX_VALUE;
}
