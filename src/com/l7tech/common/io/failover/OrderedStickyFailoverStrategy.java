/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io.failover;

import java.util.HashMap;
import java.util.Map;

/**
 * This strategy does no loadbalancing at all -- just failover.  It prefers server that are first in the server list.
 * If using a backup server, it will check every few minutes to see if a more-preferred server has come back up.
 */
public class OrderedStickyFailoverStrategy extends AbstractFailoverStrategy {
    public static final long DEFAULT_PROBE_TIME = 15 * 60 * 1000; // probe every 15 min by default

    private long probeTime = DEFAULT_PROBE_TIME;

    private int current = 0;
    private long lastProbeTime = -1;

    private int probeDelay;
    private int probing = -1;

    private Map index = new HashMap();

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers servers to use.  Must not be null or empty.
     */
    public OrderedStickyFailoverStrategy(Object[] servers) {
        super(servers);
        for (int i = 0; i < servers.length; i++) {
            Object server = servers[i];
            index.put(server, new Integer(i));
        }
    }

    public long getProbeTime() {
        return probeTime;
    }

    public void setProbeTime(long probeTime) {
        if (probeTime < 1) throw new IllegalArgumentException("probeTime must be positive");
        this.probeTime = probeTime;
    }

    public Object selectService() {

        if (current > 0) {
            if (probing >= 0) {
                // We are currently probing.  See if it is time to probe the next server.
                if (--probeDelay < 0) {
                    probeDelay = servers.length;
                    if (++probing < current)
                        return servers[probing];
                    probing = -1; // finished probing
                }
            } else {
                // See if it is time to start probing to see if any higher-preference server has come back up.
                final long now = System.currentTimeMillis();
                if (now - lastProbeTime > probeTime) {
                    lastProbeTime = now;
                    probing = 0;
                    probeDelay = servers.length;
                    return servers[probing];
                }
            }
        }

        return servers[current];
    }

    private int index(Object service) {
        Integer i = (Integer)index.get(service);
        if (i == null) throw new IllegalArgumentException("The service '" + service + "' is not known to this failover strategy");
        return i.intValue();
    }

    public void reportFailure(Object service) {
        if (index(service) == current) {
            if (current >= servers.length) {
                current = 0;
            } else {
                current++;
                if (lastProbeTime < 0)
                    lastProbeTime = System.currentTimeMillis(); // suppress immediate re-probe of just-known-to-be-down server
            }
        }
    }

    public void reportSuccess(Object service) {
        int i = index(service);
        if (i < current)
            current = i;
    }

    public String getName() {
        return "ordered";
    }

    public String getDescription() {
        return "Ordered Sticky with Failover";
    }
}
