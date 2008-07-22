/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;

/** @author alex */
public class JmsEndpointManagerStub extends EntityManagerStub<JmsEndpoint, EntityHeader> implements JmsEndpointManager {
    public Collection findMessageSourceEndpoints() throws FindException {
        throw new UnsupportedOperationException();
    }

    public JmsEndpoint[] findEndpointsForConnection(long connectionOid) throws FindException {
        throw new UnsupportedOperationException();
    }

    public EntityHeader[] findEndpointHeadersForConnection(long connectionOid) throws FindException {
        throw new UnsupportedOperationException();
    }
}
