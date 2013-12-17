package com.l7tech.server.transport;

import com.l7tech.common.io.PortRanges;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnectorHeader;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author ghuang
 */
public class SsgConnectorManagerStub extends EntityManagerStub<SsgConnector, EntityHeader> implements SsgConnectorManager {

    List<TransportDescriptor> transportDescriptors = new ArrayList<>();
    public SsgConnectorManagerStub() {
        super();
    }

    public SsgConnectorManagerStub(SsgConnector... connectors) {
        super(connectors);
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
        return new TransportDescriptor[0];
    }
}
