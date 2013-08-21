/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;
import java.util.logging.Logger;

/**
 * Hibernate manager for JMS connections and endpoints.  Endpoints cannot be found
 * directly using this class, only by reference from their associated Connection.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsConnectionManagerImpl
        extends HibernateEntityManager<JmsConnection, EntityHeader>
        implements InitializingBean, JmsConnectionManager
{
    @Override
    public EnumSet<JmsProviderType> findAllProviders() throws FindException {
        return EnumSet.allOf(JmsProviderType.class);
    }

    @Override
    public void delete( Goid goid ) throws DeleteException, FindException {
        findAndDelete( goid );
    }

    /**
     * Deletes a {@link JmsConnection} and all associated {@link com.l7tech.gateway.common.transport.jms.JmsEndpoint}s.
     * <p/>
     * Must be called within a transaction!
     *
     * @param connection the object to be deleted.
     * @throws DeleteException if the connection, or one of its dependent endpoints, cannot be deleted.
     */
    @Override
    public void delete(final JmsConnection connection) throws DeleteException {
        _logger.info("Deleting JmsConnection " + connection);

        try {
            EntityHeader[] endpoints = jmsEndpointManager.findEndpointHeadersForConnection(connection.getGoid());

            for (EntityHeader endpoint : endpoints)
                jmsEndpointManager.delete(endpoint.getGoid());

            super.delete(connection);
        } catch (Exception e) {
            throw new DeleteException(e.toString(), e);
        }
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    @Override
    public Class<JmsConnection> getImpClass() {
        return JmsConnection.class;
    }

    @Override
    public Class<JmsConnection> getInterfaceClass() {
        return JmsConnection.class;
    }

    public String getTableName() {
        return "jms_connection";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.JMS_CONNECTION;
    }

    private JmsEndpointManager jmsEndpointManager;

    public void setJmsEndpointManager(JmsEndpointManager jmsEndpointManager) {
        this.jmsEndpointManager = jmsEndpointManager;
    }

    @Override
    protected void initDao() throws Exception {
        if (jmsEndpointManager == null) {
            throw new IllegalArgumentException("Endpoint Manager is required");
        }
    }

    private final Logger _logger = Logger.getLogger(getClass().getName());
}
