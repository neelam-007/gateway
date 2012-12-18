package com.l7tech.server.transport;

import com.l7tech.common.io.PortRanges;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SsgConnectionManagerStub extends EntityManagerStub<SsgConnector, EntityHeader> implements SsgConnectorManager {

    public SsgConnectionManagerStub(final SsgConnector... entitiesIn) {
        super( entitiesIn );
    }

    @Override
    public PortRanges getReservedPorts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String translateBindAddress(String bindAddress, int port) throws ListenerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerTransportProtocol(TransportDescriptor transportDescriptor, TransportModule transportModule) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransportModule unregisterTransportProtocol(String scheme) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransportDescriptor[] getTransportProtocols() {
        try {
            List<TransportDescriptor> ret = new ArrayList<TransportDescriptor>();
            for(SsgConnector connector: findAll()){
                ret.add(new TransportDescriptor(connector.getScheme(),connector.isSecure()));
            }
            return ret.toArray(new TransportDescriptor[ret.size()]);
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return SsgConnector.class;
    }

}
