/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.JmsEndpointHeader;

import java.util.Collection;

/**
 * @author alex
 */
public interface JmsEndpointManager extends PropertySearchableEntityManager<JmsEndpointHeader>, EntityManager<JmsEndpoint, JmsEndpointHeader> {
    Collection findMessageSourceEndpoints() throws FindException;
    JmsEndpoint[] findEndpointsForConnection(Goid connectionGoid) throws FindException;
    JmsEndpointHeader[] findEndpointHeadersForConnection(Goid connectionGoid) throws FindException;
}
