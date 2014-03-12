package com.l7tech.server.bundling;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.util.CollectionUtils;

import java.util.List;

/**
 * For holding jms entities.
 */
public class JmsContainer extends PersistentEntityContainer<JmsEndpoint> {
    private final JmsConnection connection;

    public JmsContainer(final JmsEndpoint endpoint, final JmsConnection connection) {
        super(endpoint);
        this.connection = connection;
    }

    public JmsEndpoint getJmsEndpoint() {
        return entity;
    }

    public JmsConnection getJmsConnection() {
        return connection;
    }

    @Override
    public List getEntities() {
        return CollectionUtils.list(entity, connection);
    }
}