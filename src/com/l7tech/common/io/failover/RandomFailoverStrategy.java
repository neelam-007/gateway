/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io.failover;

import java.util.Random;


/**
 * A {@link FailoverStrategy} that picks a service randomly.
 */
public class RandomFailoverStrategy extends AbstractFailoverStrategy {
    private final Random random = new Random();

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers servers to use.  Must not be null or empty.
     */
    public RandomFailoverStrategy(Object[] servers) {
        super(servers);
    }

    public Object selectService() {
        return servers[random.nextInt(servers.length)];  
    }

    public void reportFailure(Object service) {
        // ignored
    }

    public void reportSuccess(Object service) {
        // ignored
    }
}
