/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io.failover;

/**
 * Represents a strategy for selecting a server to use for a particular connection.  The actual servers are represented
 * by opaque Object tokens whose precise type is unimportant to the strategy -- only the reported success or failure
 * of past connection attempts matters to a FailoverStrategy.
 * <p>
 * Different strategies can be implemented with different goals: round-robin, random, sticky-with-failover, or
 * whatever else.  Users of a strategy can provide feedback hints about requests that succeed or fail.
 */
public interface FailoverStrategy {
    /**
     * Select a service to use for a request.  Returns the "best" service to use for the next request
     * according to this strategy's current state.
     * <p>
     * Note that a concrete implementation might never signal "give up" by returning null.
     * Therefore a caller that repeatedly calls selectService() and attempts a request until one succeeds
     * is strongly advised to implement their own retry limit to avoid looping forever.
     *
     * @return a service object to use, or null to give up for now.
     */
    Object selectService();

    /**
     * Report that the specified server has failed to answer a request.
     *
     * @param service  the server that failed.  Never null.
     * @throws IllegalArgumentException if the specified server is unknown to or unmanaged by this strategy instance.
     */
    void reportFailure(Object service);

    /**
     * Report that the specified server has successfully answered a request.
     *
     * @param service  the server that succeeded.  Never null.
     * @throws IllegalArgumentException if the specified server is unknown to or unmanaged by this strategy instance.
     */
    void reportSuccess(Object service);

    /** @return the short symbolic name of this FailoverStrategy. */
    String getName();

    /** @return the longer human-readable name of this FailoverStrategy. */
    String getDescription();
}
