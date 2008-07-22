/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io.failover;


/**
 * Superclass for implementations of {@link FailoverStrategy}.
 */
public abstract class AbstractFailoverStrategy implements FailoverStrategy {
    protected final Object[] servers;

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers  servers to use.  Must not be null or empty.
     */
    protected AbstractFailoverStrategy(Object[] servers) {
        if (servers == null) throw new NullPointerException();
        if (servers.length < 1) throw new IllegalArgumentException("Must be at least one server");
        this.servers = servers;
    }

    abstract public Object selectService();

    abstract public void reportFailure(Object service);

    abstract public void reportSuccess(Object service);

    public String toString() {
        return getDescription();
    }

    /**
     * Synchronizes an existing unsynchronized {@link FailoverStrategy}.
     *
     * @param strat the strategy that should be synchronized.  Must not be null.
     * @return strat wrapped in a layer that synchronizes access.  Never null.
     */
    public static FailoverStrategy makeSynchronized(final FailoverStrategy strat) {
        return new FailoverStrategy() {
            public synchronized Object selectService() {
                return strat.selectService();
            }

            public synchronized void reportFailure(Object service) {
                strat.reportFailure(service);
            }

            public synchronized void reportSuccess(Object service) {
                strat.reportSuccess(service);
            }

            public synchronized String getName() {
                return strat.getName();
            }

            public synchronized String getDescription() {
                return strat.getDescription();
            }
        };
    }
}
