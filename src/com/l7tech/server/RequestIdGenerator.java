/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.server.ServerConfig;

import java.math.BigInteger;

/**
 * Generates reasonably-unique request identifiers incorporating a server ID, (see {@link ServerConfig#getServerId()}) a boot time and sequence number.
 *
 * @author alex
 * @version $Revision$
 */
public class RequestIdGenerator {
    public static BigInteger next() {
        RequestIdGenerator instance = getInstance();
        long seq;
        BigInteger next;
        synchronized( instance ) {
            seq = instance.nextSequence();
            next = new BigInteger( instance._seed.toString() );
        }
        return next.add( new BigInteger( new Long( seq ).toString() ) );
    }

    private static synchronized RequestIdGenerator getInstance() {
        if ( _instance == null ) _instance = new RequestIdGenerator();
        return _instance;
    }

    private void reseed( long time ) {
        BigInteger seedServerId = new BigInteger( new Byte( _serverId ).toString() ).shiftLeft(120);
        BigInteger bigTime = new BigInteger( new Long( time ).toString() ).shiftLeft(72);

        _seed = seedServerId.or( bigTime );
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

    private BigInteger _seed;
    private byte _serverId;
    private long _sequence;

    private static RequestIdGenerator _instance;
    public static final long MAX_SEQ = 0xffffffffffffffL; // 2^56 - 1
}
