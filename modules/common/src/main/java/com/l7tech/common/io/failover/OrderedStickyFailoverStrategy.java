package com.l7tech.common.io.failover;

import java.util.HashMap;
import java.util.Map;

/**
 * This strategy does no loadbalancing at all -- just failover.  It prefers server that are first in the server list.
 * If using a backup server, it will check every few minutes to see if a more-preferred server has come back up.
 */
public class OrderedStickyFailoverStrategy<ST> extends AbstractFailoverStrategy<ST> {
    public static final long DEFAULT_PROBE_TIME = 15 * 60 * 1000; // probe every 15 min by default

    private int current = 0;
    private long lastProbeTime = -1;

    private long probeTime = -1;
    private int probeDelay;
    private int probing = -1;
    private int nextdown = -1;

    private Map<ST, Integer> index = new HashMap<ST, Integer>();

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers servers to use.  Must not be null or empty.
     */
    public OrderedStickyFailoverStrategy(ST[] servers) {
        super(servers);
        for (int i = 0; i < servers.length; i++) {
            ST server = servers[i];
            index.put(server, i);
        }
    }

    /**
     * Get the probe time or -1 if not set.
     *
     * @return The probe time.
     */
    public long getProbeTime() {
        return probeTime;
    }

    public void setProbeTime(long probeTime) {
        if (probeTime < 1 && probeTime != -1) throw new IllegalArgumentException("probeTime must be positive");
        this.probeTime = probeTime;
    }

    @Override
    public ST selectService() {

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
                final long now = timeSource.currentTimeMillis();
                if (now - lastProbeTime > getProbeInterval()) {
                    lastProbeTime = now;
                    probing = 0;
                    probeDelay = servers.length;
                    return servers[probing];
                }
            }
        }

        if (current >= servers.length) {
            // Nothing is up, so cycle through all downed servers
            if (nextdown < 0 || nextdown >= servers.length)
                nextdown = 0;
            return servers[nextdown++];
        }

        return servers[current];
    }

    private int index(ST service) {
        Integer i = index.get(service);
        if (i == null) throw new IllegalArgumentException("The service '" + service + "' is not known to this failover strategy");
        return i;
    }

    @Override
    public void reportFailure(ST service) {
        if (index(service) == current) {
            if (current >= servers.length) {
                current = 0;
            } else {
                current++;
                if (lastProbeTime < 0)
                    lastProbeTime = timeSource.currentTimeMillis(); // suppress immediate re-probe of just-known-to-be-down server
            }
        }
    }

    @Override
    public void reportSuccess(ST service) {
        int i = index(service);
        if (i < current)
            current = i;
    }

    @Override
    public String getName() {
        return "ordered";
    }

    @Override
    public String getDescription() {
        return "Ordered Sticky with Failover";
    }

    private long getProbeInterval() {
        return probeTime > 0 ?
                probeTime :
                super.getProbeInterval( DEFAULT_PROBE_TIME );
    }

}
