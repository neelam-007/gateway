/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io.failover;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory for failover strategies.
 */
public class FailoverStrategyFactory {
    /** fake server list for creating prototype strategy instances. */
    private static final String[] FS = new String[] { "a", "b", "c" };

    /**
     * Define the static strategy
     */
    public static final OrderedStickyFailoverStrategy<?> ORDERED = new OrderedStickyFailoverStrategy<String>(FS);
    public static final StickyFailoverStrategy<?> STICKY = new StickyFailoverStrategy<String>(FS);
    public static final RoundRobinFailoverStrategy<?> ROBIN = new RoundRobinFailoverStrategy<String>(FS);
    public static final RandomFailoverStrategy<?> RANDOM = new RandomFailoverStrategy<String>(FS);

    /**
     * Container for dynamic strategy, we can register or unregister strategy in runtime
     */
    public final ConcurrentMap<String, FailoverStrategy> strategies = new ConcurrentHashMap<String, FailoverStrategy>();
    

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
     * Register new failover strategy. If the strategy is already registered, will replace with the provided strategy.
     *
     * @param strategy The Failover strategy
     */
    public <ST> void registerStrategy(FailoverStrategy strategy) {
        strategies.put(strategy.getName(), strategy);
    }

    /**
     * Unregister failover strategy. No effect if the strategy is not registered before .
     *
     * @param strategy The Failover strategy
     */
    public <ST> void unregisterStrategy(FailoverStrategy strategy) {
        strategies.remove(strategy.getName());
    }

    /**
     * Retrieve all the FailoverStrategy in alphabetical order with the strategy name,
     * The return strategy included static and dynamic strategy.
     *
     * @return Failover strategies included static and dynamic strategy
     */
    public FailoverStrategy[] getAllFailoverStrategy() {
        FailoverStrategy[] fss = new FailoverStrategy[getFailoverStrategyNames().length + strategies.size()];
        System.arraycopy(getFailoverStrategyNames(), 0, fss, 0, getFailoverStrategyNames().length);

        SortedMap<String, FailoverStrategy> map = new TreeMap<String, FailoverStrategy>();
        for (Iterator iterator = strategies.keySet().iterator(); iterator.hasNext(); ) {
            Object next =  iterator.next();
            map.put((String) next, strategies.get(next));
        }
        System.arraycopy(map.values().toArray(new FailoverStrategy[strategies.size()]),
                0, fss, getFailoverStrategyNames().length, strategies.size());
        return fss;
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
        if (STICKY.getName().equalsIgnoreCase(name)) return new StickyFailoverStrategy<ST>(servers);
        if (ROBIN.getName().equalsIgnoreCase(name)) return new RoundRobinFailoverStrategy<ST>(servers);
        if (RANDOM.getName().equalsIgnoreCase(name)) return new RandomFailoverStrategy<ST>(servers);
        throw new IllegalArgumentException("Unknown failover strategy name: " + name);
    }

    /**
     * Create the specified FailoverStrategy given the name of the strategy.
     *
     * @param name    the name of the strategy to create.
     * @param servers the server list.  Must not be null or empty.
     * @return the newly-created strategy.
     * @throws IllegalArgumentException if the specified stratey name is not recognized.
     */
    public <ST> FailoverStrategy<ST> createFailoverStrategy(String name, ST[] servers, Map properties) {
        FailoverStrategy fs = null;
        fs = strategies.get(name);

        if (fs == null) {
            if (ORDERED.getName().equalsIgnoreCase(name)) {
                fs = new OrderedStickyFailoverStrategy<ST>(servers);
            }
            if (STICKY.getName().equalsIgnoreCase(name)) {
                fs =  new StickyFailoverStrategy<ST>(servers);
            }
            if (ROBIN.getName().equalsIgnoreCase(name)) {
                fs =  new RoundRobinFailoverStrategy<ST>(servers);
            }
            if (RANDOM.getName().equalsIgnoreCase(name)) {
                fs = new RandomFailoverStrategy<ST>(servers);
            }
        } else {
            fs = createFailoverStrategy(fs.getClass(), servers);
        }

        if (fs!= null) {
            if (fs instanceof ConfigurableFailoverStrategy) {
                ((ConfigurableFailoverStrategy)fs).setProperties(properties);
            }
            return fs;
        }

        throw new IllegalArgumentException("Unknown failover strategy name: " + name);
    }

    /**
     * Create the Failover strategy. The Strategy is expected to have only one constructor with servers as parameter.
     *
     * @param strategy The Strategy Class
     * @param servers The server list
     * @param <ST>
     * @return The FailoverStrategy
     */
    private <ST> FailoverStrategy<ST> createFailoverStrategy(Class strategy, ST[] servers) {
        try {
            Constructor c = strategy.getConstructors()[0];
            return (FailoverStrategy) c.newInstance(new Object[] {servers});
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
