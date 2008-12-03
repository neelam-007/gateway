/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io.failover;

import java.util.*;

/**
 * A {@link FailoverStrategy} that picks a server at random and uses it
 * until it goes down, at which point it picks another one.  If none are left that haven't been reported as failed,
 * retries the least-recently-reported-failed server.
 * <p>
 * This implementation is unsynchronized; see {@link AbstractFailoverStrategy#makeSynchronized}.
 */
public class StickyFailoverStrategy<ST> extends AbstractFailoverStrategy<ST> {
    private final Set<ST> up;
    private final Set<ST> down;

    /**
     * Create a StickyFailoverStrategy using the specified server list.
     *
     * @param servers the list of servers.  Must not be null or empty.  The precise type used to
     *        represent a server does not matter to this strategy.
     */
    public StickyFailoverStrategy(ST[] servers) {
        super(servers);
        List<ST> permuted = new ArrayList<ST>(Arrays.asList(servers));
        Collections.shuffle(permuted);
        up = new LinkedHashSet<ST>(permuted);
        down = new LinkedHashSet<ST>();
    }

    @Override
    public ST selectService() {
        if (!up.isEmpty())
            return up.iterator().next();
        return down.iterator().next();
    }

    @Override
    public void reportFailure(ST service) {
        // Remove from up; move to last place in down
        up.remove(service);
        down.remove(service);
        down.add(service);
    }

    @Override
    public void reportSuccess(ST service) {
        // Remove from down; ensure presence in up
        down.remove(service);
        up.add(service);
    }

    @Override
    public String getName() {
        return "sticky";
    }

    @Override
    public String getDescription() {
        return "Random Sticky with Failover";
    }
}
