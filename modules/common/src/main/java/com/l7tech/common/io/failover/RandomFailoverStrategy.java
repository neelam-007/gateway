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
public class RandomFailoverStrategy<ST> extends AbstractFailoverStrategy<ST> {
    private final Random random = new Random();

    /**
     * Create a new instance based on the specified server array, which must be non-null and non-empty.
     * The precise type of object used to represent a server does not matter to a FailoverStrategy.
     *
     * @param servers servers to use.  Must not be null or empty.
     */
    public RandomFailoverStrategy(ST[] servers) {
        super(servers);
    }

    @Override
    public ST selectService() {
        return servers[random.nextInt(servers.length)];  
    }

    @Override
    public void reportFailure(ST service) {
        // ignored
    }

    @Override
    public void reportSuccess(ST service) {
        // ignored
    }

    @Override
    public String getName() {
        return "random";
    }

    @Override
    public String getDescription() {
        return "Pure Random";
    }
}
