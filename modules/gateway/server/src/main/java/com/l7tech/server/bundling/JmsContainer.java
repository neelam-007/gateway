package com.l7tech.server.bundling;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Entity container for holding jms entities.
 */
public class JmsContainer extends EntityContainer<JmsEndpoint> {
    @NotNull
    private final JmsConnection connection;

    /**
     * Creates a new JMS entity container
     *
     * @param endpoint   The JmsEndpoint
     * @param connection The related JmsConnection
     */
    public JmsContainer(@NotNull final JmsEndpoint endpoint, @NotNull final JmsConnection connection) {
        super(endpoint);
        this.connection = connection;
    }

    /**
     * Returns the jsm endpoint
     *
     * @return returns the jms endpoint
     */
    @NotNull
    public JmsEndpoint getJmsEndpoint() {
        return getEntity();
    }

    /**
     * Returns the jmsConnection
     *
     * @return The JMS connection
     */
    @NotNull
    public JmsConnection getJmsConnection() {
        return connection;
    }

    @NotNull
    @Override
    public List<Entity> getEntities() {
        return Arrays.<Entity>asList(getEntity(), connection);
    }
}