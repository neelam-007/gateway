/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;

import java.util.Collection;

/**
 * @author alex
 */
public interface JmsEndpointManager extends EntityManager<JmsEndpoint, EntityHeader> {
    Collection findMessageSourceEndpoints() throws FindException;
    JmsEndpoint[] findEndpointsForConnection(long connectionOid) throws FindException;
    EntityHeader[] findEndpointHeadersForConnection(long connectionOid) throws FindException;
}
