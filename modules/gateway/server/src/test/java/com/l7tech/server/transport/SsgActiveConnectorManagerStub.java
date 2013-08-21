package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnectorHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author ghuang
 */
public class SsgActiveConnectorManagerStub extends EntityManagerStub<SsgActiveConnector, SsgActiveConnectorHeader> implements SsgActiveConnectorManager {

    public SsgActiveConnectorManagerStub() {
        super();
    }

    public SsgActiveConnectorManagerStub(SsgActiveConnector... connectors) {
        super(connectors);
    }

    @NotNull
    @Override
    public Collection<SsgActiveConnector> findSsgActiveConnectorsByType(@NotNull String type) throws FindException {
        return null;
    }

}