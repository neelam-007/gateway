/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io.failover;

import com.l7tech.util.SyspropUtil;

import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * A {@link FailoverStrategy} that tries all "up" services in round-robin order, using up/down hints to move
 * services betweent he "up" and "down" lists.
 * <p>
 * This implementation is unsynchronized; see {@link AbstractFailoverStrategy#makeSynchronized}.
 */
public class RoundRobinFailoverStrategy<ST> extends AbstractFailoverStrategy<ST> {
    private static final Logger logger = Logger.getLogger(RoundRobinFailoverStrategy.class.getName());
    private static final long DEFAULT_PROBE_MILLIS = 5 * 60 * 1000; // Retry failed server every 5 min by default

    private long probeTime = SyspropUtil.getLong("com.l7tech.common.io.failover.robin.retryMillis", DEFAULT_PROBE_MILLIS);

    int next = 0;
    LinkedHashMap<ST, Long> up = new LinkedHashMap<ST, Long>();
    LinkedHashMap<ST, Long> down = new LinkedHashMap<ST, Long>();

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers servers to use.  Must not be null or empty.
     */
    public RoundRobinFailoverStrategy(ST[] servers) {
        super(servers);
        for (ST server : servers)
            up.put(server, null);
        down.clear();
    }

    @Override
    public ST selectService() {
        if (!down.isEmpty()) {
            // Check if it's time to probe one of the downed servers
            long now = System.currentTimeMillis();
            final Iterator<Map.Entry<ST, Long>> entryIterator = down.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<ST, Long> entry = entryIterator.next();
                ST server = entry.getKey();
                Long probeWhen = entry.getValue();
                if (probeWhen != null && probeWhen <= now) {
                    // Probe this server; update time, move to end of list, and return it
                    if (logger.isLoggable(Level.FINE)) logger.finer("Probing server: " + server);
                    entryIterator.remove();
                    down.put(server, now + probeTime);
                    return server;
                }
            }
        }

        // Round robin through UP list, unless all are down, in which case we robin through DOWN list
        LinkedHashMap<ST, Long> toUse = up.isEmpty() ? down : up;
        assert !toUse.isEmpty();  // Must always be at least 1 server, whether its up or down

        // Select first server
        Iterator<Map.Entry<ST,Long>> i = toUse.entrySet().iterator();
        Map.Entry<ST,Long> entry = i.next();
        ST server = entry.getKey();
        Long value = entry.getValue();

        // Move it to end of list
        i.remove();
        toUse.put(server, value);

        return server;
    }

    @Override
    public void reportFailure(ST service) {
        if (up.isEmpty() || !up.containsKey(service)) return;
        up.remove(service);
        down.put(service, System.currentTimeMillis() + probeTime);
    }

    @Override
    public void reportSuccess(ST service) {
        if (down.isEmpty() || !down.containsKey(service)) return;
        down.remove(service);
        up.put(service, null);
    }

    @Override
    public String getName() {
        return "robin";
    }

    @Override
    public String getDescription() {
        return "Round-Robin";
    }
}
