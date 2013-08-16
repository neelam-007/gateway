/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.transport.jms;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.JmsMessagePropertyRule;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.EnumSet;

import static com.l7tech.objectmodel.EntityType.JMS_CONNECTION;
import static com.l7tech.objectmodel.EntityType.JMS_ENDPOINT;

/**
 * The SecureSpan Gateway's API for managing JMS connections and endpoints.
 */
@Secured
@Administrative
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public interface JmsAdmin {
    /**
     * Holds a tuple of ({@link JmsConnection}, {@link JmsEndpoint}).
     */
    public static final class JmsTuple implements Serializable {
        public JmsTuple( final JmsConnection jmsConnection,
                         final JmsEndpoint jmsEndpoint ) {
            if ( jmsConnection == null ) throw new IllegalArgumentException("jmsConnection is required");
            if ( jmsEndpoint == null ) throw new IllegalArgumentException("jmsEndpoint is required");
            this.connection = jmsConnection;
            this.endpoint = jmsEndpoint;
        }

        /**
         * Get the JmsConnection.
         *
         * @return The connection (never null)
         */
        public JmsConnection getConnection() {
            return connection;
        }

        /**
         * Get the JmsEndpoint.
         *
         * @return The endpoint (never null)
         */
        public JmsEndpoint getEndpoint() {
            return endpoint;
        }

        public boolean isTemplate() {
            return connection.isTemplate() || endpoint.isTemplate();
        }

        private final JmsConnection connection;
        private final JmsEndpoint endpoint;
    }

    /**
     * Retrieves the array of {@link JmsProviderType}s known to the SSG.  Note that a {@link JmsProviderType} is only a
     * container for a set of representative default settings, and the presence of a {@link JmsProviderType} in this list
     * does not guarantee that a particular JMS provider is supported by this SSG.
     *
     * @return an EnumSet of {@link JmsProviderType} records
     * @throws FindException   if a database problem prevented the providers from being retrieved
     */
    @Transactional(readOnly=true)
    EnumSet<JmsProviderType> getProviderTypes() throws FindException;

    /**
     * Finds all {@link JmsConnection}s in the database.
     *
     * @return an array of {@link JmsConnection}s
     * @throws FindException   if a database problem prevented the connections from being retrieved
     */
    @Transactional(readOnly=true)
    JmsConnection[] findAllConnections() throws FindException;

    /**
     * Finds all {@link JmsEndpoint}s in the database, each wrapped in a {@link JmsTuple} that also
     * includes its associated {@link JmsConnection}.
     *
     * @return An array of {@link JmsTuple}s. Never null.
     * @throws FindException   if a database problem prevented the endpoints and/or connections from being retrieved
     */
    @Transactional(readOnly=true)
    @Secured(types=JMS_ENDPOINT, stereotype=MethodStereotype.FIND_ENTITIES, customEntityTranslatorClassName="com.l7tech.server.admin.JmsTupleEntityTranslator")
    JmsTuple[] findAllTuples() throws FindException;

    /**
     * Finds the {@link JmsConnection} with the given GOID.
     *
     * @param goid the GOID of the connection to retrieve
     * @return the {@link JmsConnection} with the specified OID, or null if no such connection could be found
     * @throws FindException   if a database problem prevented the connection from being retrieved
     */
    @Transactional(readOnly=true)
    @Secured(types=JMS_CONNECTION, stereotype=MethodStereotype.FIND_ENTITY)
    JmsConnection findConnectionByPrimaryKey(Goid goid) throws FindException;

    /**
     * Finds the {@link JmsEndpoint} with the given GOID.
     *
     * @param goid the GOID of the endpoint to retrieve
     * @return the {@link JmsEndpoint} with the specified OID, or null if no such endpoint could be found
     * @throws FindException   if a database problem prevented the endpoint from being retrieved
     */
    @Transactional(readOnly=true)
    @Secured(types=JMS_ENDPOINT, stereotype=MethodStereotype.FIND_ENTITY)
    JmsEndpoint findEndpointByPrimaryKey(Goid goid) throws FindException;

    /**
     * Sets a flag indicating whether the {@link JmsEndpoint} with the specified GOID is a message source
     * (i.e. should be polled for inbound messages) or not (i.e. is used for outbound messages)
     *
     * @param goid             the GOID of the {@link JmsEndpoint} to update
     * @param isMessageSource true if the endpoint with the specified OID should be polled by the SSG, or false if the SSG can use this endpoint to send outbound messages.
     * @throws FindException   if a database problem prevented the endpoint from being retrieved
     * @throws UpdateException if a database problem prevented the endpoint from being updated
     */
    @Secured(types=JMS_ENDPOINT, stereotype= MethodStereotype.SET_PROPERTY_BY_ID)
    void setEndpointMessageSource(Goid goid, boolean isMessageSource) throws FindException, UpdateException;

    /**
     * Save the specified JmsConnection to the database.
     *
     * @param connection the JmsConnection to save
     * @return the GOID assigned to the saved JmsConnection.
     * @throws SaveException   if a database problem prevented the specified JmsConnection from being saved
     */
    @Secured(types=JMS_CONNECTION, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    Goid saveConnection(JmsConnection connection) throws SaveException, VersionException;

    /**
     * Save the specified JmsEndpoint to the database.
     *
     * @param endpoint the JmsEndpoint to save
     * @return the GOID assigned to the saved JmsEndpoint.
     * @throws SaveException   if a database problem prevented the specified JmsEndpoint from being saved
     */
    @Secured(types=JMS_ENDPOINT, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    Goid saveEndpoint(JmsEndpoint endpoint) throws SaveException, VersionException;

    /**
     * Deletes the {@link JmsEndpoint} with the specified OID from the database.
     *
     * @param endpointGoid the GOID of the {@link JmsEndpoint} to be deleted
     * @throws FindException   if a database problem prevented the specified JmsEndpoint from being retrieved
     * @throws DeleteException if a database problem prevented the specified JmsEndpoint from being deleted
     */
    @Secured(types=JMS_ENDPOINT, stereotype= MethodStereotype.DELETE_BY_ID)
    void deleteEndpoint(Goid endpointGoid) throws FindException, DeleteException;

    /**
     * Deletes the {@link JmsConnection} with the specified GOID from the database.
     *
     * @param connectionGoid the GOID of the {@link JmsConnection} to be deleted
     * @throws FindException   if a database problem prevented the specified JmsConnection from being retrieved
     * @throws DeleteException if a database problem prevented the specified JmsConnection from being deleted
     */
    @Secured(types=JMS_CONNECTION, stereotype= MethodStereotype.DELETE_BY_ID)
    void deleteConnection(Goid connectionGoid) throws FindException, DeleteException;

    /**
     * Returns an array of {@link JmsEndpoint}s that belong to the {@link JmsConnection} with the provided GOID.
     * <p/>
     * Returns transient instances that are not enrolled in the Hibernate session.
     *
     * @param connectionGoid
     * @return an array of {@link JmsEndpoint}s. Never null.
     * @throws FindException   if a database problem prevented the endpoints from being retrieved
     */
    @Transactional(readOnly=true)
    @Secured(types=JMS_ENDPOINT, stereotype=MethodStereotype.FIND_ENTITIES)
    JmsEndpoint[] getEndpointsForConnection(Goid connectionGoid) throws FindException;

    /**
     * Test the specified JmsConnection, which may or may not exist in the database.  The Gateway will use the
     * specified settings to open a JMS connection.  If this method does not throw, the caller can assume
     * that the settings are valid.
     *
     * @param connection JmsConnection settings to test.  Might not yet have an GOID.
     * @throws JmsTestException if the test fails
     */
    @Transactional(readOnly=true)
    void testConnection(JmsConnection connection) throws JmsTestException;

    /**
     * Test the specified JmsEndpoint on the specified JmsConnection, either or both of which may or may not exist in
     * the database.  The Gateway will use the specified settings to open a JMS
     * connection and attempt to verify the existence of a Destination for this JmsEndpoint.
     *
     * @param connection JmsConnection settings to test.  Might not yet have an GOID.
     * @param endpoint   JmsEndpoint settings to test.  Might not yet have an GOID or a valid connectionOid.
     * @throws FindException   if the connection pointed to by the endpoint cannot be loaded
     */
    @Transactional(readOnly=true)
    void testEndpoint(JmsConnection connection, JmsEndpoint endpoint) throws JmsTestException, FindException;

    /**
     * Get the default value of the JMS message max bytes defined in "io.jmsMessageMaxBytes"
     * @return the maximum number of bytes permitted for a JMS message, or 0 for unlimited (Integer)
     */
    @Transactional(readOnly=true)
    long getDefaultJmsMessageMaxBytes();


    /**
     * Validate the Jms Message Property Rule.
     *
     * @param rule The Jms Message Property rule
     * @return True if the message property is valid, False if the message property is not valid
     */
    boolean isValidProperty(JmsMessagePropertyRule rule);

    /**
     * Validate the JMS dedicated pool size
     * @param size The size of the pool
     * @return True if the size is valid, False if not.
     */
    boolean isValidThreadPoolSize(String size);

    /**
     * Check to see if the dedicated thread pool is enabled
     * @return True when enable.
     */
    boolean isDedicatedThreadPoolEnabled();
}
