/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io.failover;


/**
 * Superclass for implementations of {@link FailoverStrategy}.
 */
public abstract class AbstractFailoverStrategy<ST> implements FailoverStrategy<ST> {
    protected final ST[] servers;

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers  servers to use.  Must not be null or empty.
     */
    protected AbstractFailoverStrategy(ST[] servers) {
        if (servers == null) throw new NullPointerException();
        if (servers.length < 1) throw new IllegalArgumentException("Must be at least one server");
        this.servers = servers;
    }

    @Override
    abstract public ST selectService();

    @Override
    abstract public void reportFailure(ST service);

    @Override
    abstract public void reportSuccess(ST service);

    @Override
    public String toString() {
        return getDescription();
    }

    /**
     * Synchronizes an existing unsynchronized {@link FailoverStrategy}.
     *
     * @param strat the strategy that should be synchronized.  Must not be null.
     * @return strat wrapped in a layer that synchronizes access.  Never null.
     */
    public static <ST> FailoverStrategy<ST> makeSynchronized(final FailoverStrategy<ST> strat) {
        return new FailoverStrategy<ST>() {
            @Override
            public synchronized ST selectService() {
                return strat.selectService();
            }

            @Override
            public synchronized void reportFailure(ST service) {
                strat.reportFailure(service);
            }

            @Override
            public synchronized void reportSuccess(ST service) {
                strat.reportSuccess(service);
            }

            @Override
            public synchronized String getName() {
                return strat.getName();
            }

            @Override
            public synchronized String getDescription() {
                return strat.getDescription();
            }
        };
    }
}
