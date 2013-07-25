/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.JmsEndpointHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.GoidEntityManagerStub;
import com.l7tech.util.Functions;

import java.util.Collection;

/** @author alex */
public class JmsEndpointManagerStub extends GoidEntityManagerStub<JmsEndpoint, JmsEndpointHeader> implements JmsEndpointManager {

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
    public JmsEndpoint[] findEndpointsForConnection(final Goid connectionOid) throws FindException {
        return Functions.grep( findAll(), new Functions.Unary<Boolean,JmsEndpoint>(){
            @Override
            public Boolean call( final JmsEndpoint jmsEndpoint ) {
                return jmsEndpoint.getConnectionGoid().equals(connectionOid);
            }
        } ).toArray( new JmsEndpoint[0] );
    }

    @Override
    public JmsEndpointHeader[] findEndpointHeadersForConnection(Goid connectionOid) throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public JmsEndpoint findByOldOid(final long oid) throws FindException {
        return Functions.grepFirst( findAll(), new Functions.Unary<Boolean,JmsEndpoint>(){
            @Override
            public Boolean call( final JmsEndpoint jmsEndpoint ) {
                return jmsEndpoint.getOldOid().equals(oid);
            }
        } );
    }
}
