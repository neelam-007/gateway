/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io.failover;



/**
 * A {@link FailoverStrategy} that tries each service in-turn in round-robin order, ignoring up/down hints.
 * <p>
 * This implementation is unsynchronized; see {@link AbstractFailoverStrategy#makeSynchronized}.
 */
public class RoundRobinFailoverStrategy extends AbstractFailoverStrategy {
    int next = 0;

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers servers to use.  Must not be null or empty.
     */
    public RoundRobinFailoverStrategy(Object[] servers) {
        super(servers);
    }

    public Object selectService() {
        Object service = servers[next++];
        if (next >= servers.length) next = 0;
        return service;
    }

    public void reportFailure(Object service) {
        // ignored
    }

    public void reportSuccess(Object service) {
        // ignored
    }

    public String getName() {
        return "robin";
    }

    public String getDescription() {
        return "Pure Round-Robin";
    }
}
