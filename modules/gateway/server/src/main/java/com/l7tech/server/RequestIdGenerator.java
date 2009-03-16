/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.gateway.common.RequestId;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates probably-unique request identifiers incorporating a server ID,
 * (see {@link ServerConfig#getServerId()}) a boot time and sequence number.
 *
 * @author alex
 * @version $Revision$
 */
public class RequestIdGenerator {
    public static RequestId next() {
        return _instance.doNext();
    }

    RequestId doNext() {
        long time = _time.get();
        long seq = _sequence.incrementAndGet();
        if (seq > MAX_SEQ) {
            _time.compareAndSet(time, System.currentTimeMillis());
            _sequence.compareAndSet(seq, 0);
        }
        return new RequestId(time, seq);
    }

    RequestIdGenerator() {
        this(ServerConfig.getInstance().getServerBootTime(), 0);
    }

    RequestIdGenerator(long startTime, long startSeq) {
        _time.set(startTime);
        _sequence.set(startSeq);
    }

    private final AtomicLong _time = new AtomicLong();
    private final AtomicLong _sequence = new AtomicLong();

    private static final RequestIdGenerator _instance = new RequestIdGenerator();
    public static final long MAX_SEQ = Long.MAX_VALUE / 2;
}
