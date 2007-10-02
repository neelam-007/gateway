/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io.failover;

import com.l7tech.common.util.SyspropUtil;

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
public class RoundRobinFailoverStrategy extends AbstractFailoverStrategy {
    private static final Logger logger = Logger.getLogger(RoundRobinFailoverStrategy.class.getName());
    private static final long DEFAULT_PROBE_MILLIS = 5 * 60 * 1000; // Retry failed server every 5 min by default

    private long probeTime = SyspropUtil.getLong("com.l7tech.common.io.failover.robin.retryMillis", DEFAULT_PROBE_MILLIS).longValue();

    int next = 0;
    LinkedHashMap up = new LinkedHashMap();
    LinkedHashMap down = new LinkedHashMap();

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers servers to use.  Must not be null or empty.
     */
    public RoundRobinFailoverStrategy(Object[] servers) {
        super(servers);
        for (int i = 0; i < servers.length; i++) {
            Object server = servers[i];
            up.put(server, null);
        }
        down.clear();
    }

    public Object selectService() {
        if (!down.isEmpty()) {
            // Check if it's time to probe one of the downed servers
            long now = System.currentTimeMillis();
            for (Iterator i = down.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                Object server = entry.getKey();
                Long probeWhen = (Long)entry.getValue();
                if (probeWhen != null && probeWhen.longValue() <= now) {
                    // Probe this server; update time, move to end of list, and return it
                    if (logger.isLoggable(Level.FINE)) logger.finer("Probing server: " + server);
                    i.remove();
                    down.put(server, new Long(now + probeTime));
                    return server;
                }
            }
        }

        // Round robin through UP list, unless all are down, in which case we robin through DOWN list
        LinkedHashMap toUse = up.isEmpty() ? down : up;
        assert !toUse.isEmpty();  // Must always be at least 1 server, whether its up or down

        // Select first server
        Iterator i = toUse.entrySet().iterator();
        Map.Entry entry = (Map.Entry)i.next();
        Object server = entry.getKey();
        Long value = (Long)entry.getValue();

        // Move it to end of list
        i.remove();
        toUse.put(server, value);

        return server;
    }

    public void reportFailure(Object service) {
        if (up.isEmpty() || !up.containsKey(service)) return;
        up.remove(service);
        down.put(service, new Long(System.currentTimeMillis() + probeTime));
    }

    public void reportSuccess(Object service) {
        if (down.isEmpty() || !down.containsKey(service)) return;
        down.remove(service);
        up.put(service, null);
    }

    public String getName() {
        return "robin";
    }

    public String getDescription() {
        return "Round-Robin";
    }
}
