/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.objectmodel.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hibernate manager for JMS connections and endpoints.  Endpoints cannot be found
 * directly using this class, only by reference from their associated Connection.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsConnectionManager extends HibernateEntityManager
  implements ApplicationContextAware, InitializingBean {
    private List _allProviders = null;
    private ApplicationContext applicationContext;

    public Collection findAllProviders() throws FindException {
        // TODO make this real, eh?!!
        if (_allProviders == null) {
            JmsProvider openjms = new JmsProvider("OpenJMS", "org.exolab.jms.jndi.InitialContextFactory", "QueueConnectionFactory");
            JmsProvider jbossmq = new JmsProvider("JBossMQ", "org.jnp.interfaces.NamingContextFactory", "QueueConnectionFactory");
            JmsProvider mqLdap = new JmsProvider("WebSphere MQ over LDAP", "com.sun.jndi.ldap.LdapCtxFactory", "L7QueueConnectionFactory");
            List list = new ArrayList();
            list.add(openjms);
            list.add(jbossmq);
            list.add(mqLdap);
            _allProviders = list;
        }
        return _allProviders;
    }

    public JmsConnection findConnectionByPrimaryKey(long oid) throws FindException {
        return (JmsConnection)findByPrimaryKey(JmsConnection.class, oid);
    }

    public long save(final JmsConnection conn) throws SaveException {
        _logger.info("Saving JmsConnection " + conn);
        try {
            return ((Long)getHibernateTemplate().save(conn)).longValue();
        } catch (DataAccessException e) {
            throw new SaveException(e.toString(), e);
        }
    }

    public void update(final JmsConnection conn) throws VersionException, UpdateException {
        _logger.info("Updating JmsConnection " + conn);

        JmsConnection original = null;
        // check for original connection
        try {
            original = findConnectionByPrimaryKey(conn.getOid());
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


    /**
     * Deletes a {@link JmsConnection} and all associated {@link com.l7tech.common.transport.jms.JmsEndpoint}s.
     * <p/>
     * Must be called within a transaction!
     *
     * @param connection the object to be deleted.
     * @throws DeleteException if the connection, or one of its dependent endpoints, cannot be deleted.
     * @throws FindException   if the connection, or one of its dependent endpoints, cannot be found.
     */
    public void delete(final JmsConnection connection) throws DeleteException, FindException {
        _logger.info("Deleting JmsConnection " + connection);

        try {
            EntityHeader[] endpoints = jmsEndpointManager.findEndpointHeadersForConnection(connection.getOid());

            for (int i = 0; i < endpoints.length; i++)
                jmsEndpointManager.delete(endpoints[i].getOid());

            getHibernateTemplate().delete(connection);
        } catch (DataAccessException e) {
            throw new DeleteException(e.toString(), e);
        }
    }

    /**
     * Overridden to take care of dependent objects
     *
     * @param oid
     * @throws DeleteException
     * @throws FindException
     */
    public void delete(long oid) throws DeleteException, FindException {
        delete(findConnectionByPrimaryKey(oid));
    }

    public Class getImpClass() {
        return JmsConnection.class;
    }

    public Class getInterfaceClass() {
        return JmsConnection.class;
    }

    public String getTableName() {
        return "jms_connection";
    }

    private JmsEndpointManager jmsEndpointManager;

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
    }

    public void setJmsEndpointManager(JmsEndpointManager jmsEndpointManager) {
        this.jmsEndpointManager = jmsEndpointManager;
    }

    protected void initDao() throws Exception {
        if (jmsEndpointManager == null) {
            throw new IllegalArgumentException("Endpoint Manager is required");
        }
    }

    private final Logger _logger = Logger.getLogger(getClass().getName());
}
