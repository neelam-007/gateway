/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.Entity;

/**
 * Holds information needed to route a message to an outbound JMS destination.
 */
public class JmsRoutingAssertion extends RoutingAssertion {
    private Long endpointOid = null;
    private String endpointName = null;

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

    /** @return the name of this endpoint if known, for cosmetic purposes only. */
    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }
}
