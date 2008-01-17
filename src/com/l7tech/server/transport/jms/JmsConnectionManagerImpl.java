/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.objectmodel.*;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    private final List<JmsProvider> _allProviders;

    public JmsConnectionManagerImpl() {
        // TODO make this real, eh?!!
        JmsProvider tibcoEmsProvider = new JmsProvider("TIBCO EMS", "com.tibco.tibjms.naming.TibjmsInitialContextFactory", "QueueConnectionFactory");
        JmsProvider mqSeriesOverLdapProvider = new JmsProvider("WebSphere MQ over LDAP", "com.sun.jndi.ldap.LdapCtxFactory", "QueueConnectionFactory");
        List<JmsProvider> list = new ArrayList<JmsProvider>();
        list.add(tibcoEmsProvider);
        list.add(mqSeriesOverLdapProvider);
        _allProviders = Collections.unmodifiableList(list);
    }

    public Collection<JmsProvider> findAllProviders() throws FindException {
        return _allProviders;
    }

/*
    public void update(final JmsConnection conn) throws VersionException, UpdateException {
        _logger.info("Updating JmsConnection " + conn);

        JmsConnection original;
        // check for original connection
        try {
            original = findByPrimaryKey(conn.getOid());
        } catch (FindException e) {
            throw new UpdateException("could not get original connection", e);
        }

        // check version
        if (original.getVersion() != conn.getVersion()) {
            logger.severe("db connection has version: " + original.getVersion() + ". requestor connection has version: "
              + conn.getVersion());
            throw new VersionException("the connection you are trying to update is no longer valid.");
        }

        // update
        original.copyFrom(conn);
        getHibernateTemplate().update(original);
        logger.info("Updated JmsConnection #" + conn.getOid());
    }
*/

    @Override
    public void delete( long oid ) throws DeleteException, FindException {
        findAndDelete( oid );
    }

    /**
     * Deletes a {@link JmsConnection} and all associated {@link com.l7tech.common.transport.jms.JmsEndpoint}s.
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
            EntityHeader[] endpoints = jmsEndpointManager.findEndpointHeadersForConnection(connection.getOid());

            for (EntityHeader endpoint : endpoints)
                jmsEndpointManager.delete(endpoint.getOid());

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
