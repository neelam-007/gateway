/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 * Holds information needed to route a message to an outbound JMS destination.
 */
public class JmsRoutingAssertion extends RoutingAssertion {
    public static final int DEFAULT_TIMEOUT = 10000;

    /**
     * @return the OID of the JMS endpoint, or null if there isn't one.
     */
    public Long getEndpointOid() {
        return endpointOid;
    }

    /**
     * Set the OID of the JMS endpoint.  Set this to null if no endpoint is configured.
     * @param endpointOid  the OID of a JmsEndpoint instance, or null.
     */
    public void setEndpointOid(Long endpointOid) {
        this.endpointOid = endpointOid;
    }

    /**
     * The name of the {@link com.l7tech.common.transport.jms.JmsEndpoint}.
     * @return the name of this endpoint if known, for cosmetic purposes only. 
     */
    public String getEndpointName() {
        return endpointName;
    }

    /**
     * The name of the {@link com.l7tech.common.transport.jms.JmsEndpoint}.
     * @param endpointName the name of this endpoint if known, for cosmetic purposes only.
     */
    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    /**
     * The time, in milliseconds, that the SSG should wait for a response from the protected service.
     * After this timeout has lapsed, the request fails.
     *
     * Defaults to {@link JmsRoutingAssertion.DEFAULT_TIMEOUT}.
     * @return the response timeout (in milliseconds)
     */
    public int getResponseTimeout() {
        return responseTimeout;
    }

    /**
     * The time, in milliseconds, that the SSG should wait for a response from the protected service.
     * After this timeout has lapsed, the request fails.
     *
     * Defaults to {@link JmsRoutingAssertion.DEFAULT_TIMEOUT}.
     * @param responseTimeout the response timeout (in milliseconds)
     */
    public void setResponseTimeout( int responseTimeout ) {
        this.responseTimeout = responseTimeout;
    }

    private Long endpointOid = null;
    private String endpointName = null;
    private int responseTimeout = DEFAULT_TIMEOUT;
}
