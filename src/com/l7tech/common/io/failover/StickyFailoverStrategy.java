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
public class StickyFailoverStrategy extends AbstractFailoverStrategy {
    private final Set up;
    private final Set down;

    /**
     * Create a StickyFailoverStrategy using the specified server list.
     *
     * @param servers the list of servers.  Must not be null or empty.  The precise type used to
     *        represent a server does not matter to this strategy.
     */
    public StickyFailoverStrategy(Object[] servers) {
        super(servers);
        List permuted = new ArrayList(Arrays.asList(servers));
        Collections.shuffle(permuted);
        up = new LinkedHashSet(permuted);
        down = new LinkedHashSet();
    }

    public Object selectService() {
        if (!up.isEmpty())
            return up.iterator().next();
        return down.iterator().next();
    }

    public void reportFailure(Object service) {
        // Remove from up; move to last place in down
        up.remove(service);
        down.remove(service);
        down.add(service);
    }

    public void reportSuccess(Object service) {
        // Remove from down; ensure presence in up
        down.remove(service);
        up.add(service);
    }
}
