/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.JmsEndpointHeader;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;

/** @author alex */
public class JmsEndpointManagerStub extends EntityManagerStub<JmsEndpoint, JmsEndpointHeader> implements JmsEndpointManager {

    public JmsEndpointManagerStub() {
        super();
    }

    public JmsEndpointManagerStub( final JmsEndpoint... entitiesIn ) {
        super( entitiesIn );
    }

    @Override
    public Collection findMessageSourceEndpoints() throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public JmsEndpoint[] findEndpointsForConnection(long connectionOid) throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public JmsEndpointHeader[] findEndpointHeadersForConnection(long connectionOid) throws FindException {
        throw new UnsupportedOperationException();
    }
}
