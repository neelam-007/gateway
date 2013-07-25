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
public interface JmsEndpointManager extends PropertySearchableEntityManager<JmsEndpointHeader>, GoidEntityManager<JmsEndpoint, JmsEndpointHeader>  {
    Collection findMessageSourceEndpoints() throws FindException;
    JmsEndpoint[] findEndpointsForConnection(Goid connectionGoid) throws FindException;
    JmsEndpointHeader[] findEndpointHeadersForConnection(Goid connectionGoid) throws FindException;


    /**
     * Find an entity by old oid
     * @param oid the old oid to search for
     * @return the jms connection object if found.  null if not
     * @throws FindException
     */
    JmsEndpoint findByOldOid(long oid) throws FindException;
}
