/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io.failover;

/**
 * Factory for failover strategies.
 */
public class FailoverStrategyFactory {
    /** fake server list for creating prototype strategy instances. */
    private static final String[] FS = new String[] { "a", "b", "c" };

    public static final OrderedStickyFailoverStrategy ORDERED = new OrderedStickyFailoverStrategy(FS);
    public static final StickyFailoverStrategy STICKY = new StickyFailoverStrategy(FS);
    public static final RoundRobinFailoverStrategy ROBIN = new RoundRobinFailoverStrategy(FS);
    public static final RandomFailoverStrategy RANDOM = new RandomFailoverStrategy(FS);

    private static final FailoverStrategy[] STRATEGIES = new FailoverStrategy[] {
        ORDERED,
        STICKY,
        ROBIN,
    };

    /** @return the list of known FailoverStrategies. */
    public static FailoverStrategy[] getFailoverStrategyNames() {
        return STRATEGIES;
    }

    /**
     * Create the specified FailoverStrategy given the name of the strategy.
     *
     * @param name    the name of the strategy to create.
     * @param servers the server list.  Must not be null or empty.
     * @return the newly-created strategy.
     * @throws IllegalArgumentException if the specified stratey name is not recognized.
     */
    public static <ST> FailoverStrategy<ST> createFailoverStrategy(String name, ST[] servers) {
        if (ORDERED.getName().equalsIgnoreCase(name)) return new OrderedStickyFailoverStrategy<ST>(servers);
        if (STICKY.getName().equalsIgnoreCase(name)) return new StickyFailoverStrategy(servers);
        if (ROBIN.getName().equalsIgnoreCase(name)) return new RoundRobinFailoverStrategy(servers);
        if (RANDOM.getName().equalsIgnoreCase(name)) return new RandomFailoverStrategy(servers);
        throw new IllegalArgumentException("Unknown failover strategy name: " + name);
    }
}
