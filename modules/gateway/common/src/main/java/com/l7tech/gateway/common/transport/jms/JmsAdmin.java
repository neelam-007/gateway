package com.l7tech.gateway.common.transport.jms;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import static com.l7tech.gateway.common.security.rbac.EntityType.*;
import com.l7tech.gateway.common.admin.Administrative;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

/**
 * The SecureSpan Gateway's API for managaging JMS connections and endpoints.
 *
 * @author alex
 * @version $Revision$
 */
@Secured
@Administrative
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public interface JmsAdmin {
    /**
     * Holds a tuple of ({@link JmsConnection}, {@link JmsEndpoint}).
     */
    public static final class JmsTuple implements Serializable {
        public JmsTuple(JmsConnection conn, JmsEndpoint end) {
            this.connection = conn;
            this.endpoint = end;
        }

        public JmsConnection getConnection() {
            return connection;
        }

        public JmsEndpoint getEndpoint() {
            return endpoint;
        }

        private final JmsConnection connection;
        private final JmsEndpoint endpoint;
    }

    /**
     * Retrieves the array of {@link JmsProvider}s known to the SSG.  Note that a {@link JmsProvider} is only a
     * container for a set of representative default settings, and the presence of a {@link JmsProvider} in this list
     * does not guarantee that a particular JMS provider is supported by this SSG.
     *
     * @return an array of {@link JmsProvider} records
     * @throws FindException   if a database problem prevented the providers from being retrieved
     */
    @Transactional(readOnly=true)
    JmsProvider[] getProviderList() throws FindException;

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
    @Secured(types=JMS_ENDPOINT, stereotype=MethodStereotype.FIND_ENTITIES)
    JmsTuple[] findAllTuples() throws FindException;

    /**
     * Finds the {@link JmsConnection} with the given OID.
     *
     * @param oid the OID of the connection to retrieve
     * @return the {@link JmsConnection} with the specified OID, or null if no such connection could be found
     * @throws FindException   if a database problem prevented the connection from being retrieved
     */
    @Transactional(readOnly=true)
    @Secured(types=JMS_CONNECTION, stereotype=MethodStereotype.FIND_BY_PRIMARY_KEY)
    JmsConnection findConnectionByPrimaryKey(long oid) throws FindException;

    /**
     * Finds the {@link JmsEndpoint} with the given OID.
     *
     * @param oid the OID of the endpoint to retrieve
     * @return the {@link JmsEndpoint} with the specified OID, or null if no such endpoint could be found
     * @throws FindException   if a database problem prevented the endpoint from being retrieved
     */
    @Transactional(readOnly=true)
    @Secured(types=JMS_ENDPOINT, stereotype=MethodStereotype.FIND_BY_PRIMARY_KEY)
    JmsEndpoint findEndpointByPrimaryKey(long oid) throws FindException;

    /**
     * Sets a flag indicating whether the {@link JmsEndpoint} with the specified OID is a message source
     * (i.e. should be polled for inbound messages) or not (i.e. is used for outbound messages)
     *
     * @param oid             the OID of the {@link JmsEndpoint} to update
     * @param isMessageSource true if the endpoint with the specified OID should be polled by the SSG, or false if the SSG can use this endpoint to send outbound messages.
     * @throws FindException   if a database problem prevented the endpoint from being retrieved
     * @throws UpdateException if a database problem prevented the endpoint from being updated
     */
    @Secured(types=JMS_ENDPOINT, stereotype= MethodStereotype.SET_PROPERTY_BY_ID)
    void setEndpointMessageSource(long oid, boolean isMessageSource) throws FindException, UpdateException;

    /**
     * Save the specified JmsConnection to the database.
     *
     * @param connection the JmsConnection to save
     * @return the OID assigned to the saved JmsConnection.
     * @throws SaveException   if a database problem prevented the specified JmsConnection from being saved
     */
    @Secured(types=JMS_CONNECTION, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    long saveConnection(JmsConnection connection) throws SaveException, VersionException;

    /**
     * Save the specified JmsEndpoint to the database.
     *
     * @param endpoint the JmsEndpoint to save
     * @return the OID assigned to the saved JmsEndpoint.
     * @throws SaveException   if a database problem prevented the specified JmsEndpoint from being saved
     */
    @Secured(types=JMS_ENDPOINT, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    long saveEndpoint(JmsEndpoint endpoint) throws SaveException, VersionException;

    /**
     * Deletes the {@link JmsEndpoint} with the specified OID from the database.
     *
     * @param endpointOid the OID of the {@link JmsEndpoint} to be deleted
     * @throws FindException   if a database problem prevented the specified JmsEndpoint from being retrieved
     * @throws DeleteException if a database problem prevented the specified JmsEndpoint from being deleted
     */
    @Secured(types=JMS_ENDPOINT, stereotype= MethodStereotype.DELETE_BY_ID)
    void deleteEndpoint(long endpointOid) throws FindException, DeleteException;

    /**
     * Deletes the {@link JmsConnection} with the specified OID from the database.
     *
     * @param connectionOid the OID of the {@link JmsConnection} to be deleted
     * @throws FindException   if a database problem prevented the specified JmsConnection from being retrieved
     * @throws DeleteException if a database problem prevented the specified JmsConnection from being deleted
     */
    @Secured(types=JMS_CONNECTION, stereotype= MethodStereotype.DELETE_BY_ID)
    void deleteConnection(long connectionOid) throws FindException, DeleteException;

    /**
     * Returns an array of {@link JmsEndpoint}s that belong to the {@link JmsConnection} with the provided OID.
     * <p/>
     * Returns transient instances that are not enrolled in the Hibernate session.
     *
     * @param connectionOid
     * @return an array of {@link JmsEndpoint}s. Never null.
     * @throws FindException   if a database problem prevented the endpoints from being retrieved
     */
    @Transactional(readOnly=true)
    @Secured(types=JMS_ENDPOINT, stereotype=MethodStereotype.FIND_ENTITIES)
    JmsEndpoint[] getEndpointsForConnection(long connectionOid) throws FindException;

    /**
     * Test the specified JmsConnection, which may or may not exist in the database.  The Gateway will use the
     * specified settings to open a JMS connection.  If this method does not throw, the caller can assume
     * that the settings are valid.
     *
     * @param connection JmsConnection settings to test.  Might not yet have an OID.
     * @throws JmsTestException if the test fails
     */
    @Transactional(readOnly=true)
    void testConnection(JmsConnection connection) throws JmsTestException;

    /**
     * Test the specified JmsEndpoint on the specified JmsConnection, either or both of which may or may not exist in
     * the database.  The Gateway will use the specified settings to open a JMS
     * connection and attempt to verify the existence of a Destination for this JmsEndpoint.
     *
     * @param connection JmsConnection settings to test.  Might not yet have an OID.
     * @param endpoint   JmsEndpoint settings to test.  Might not yet have an OID or a valid connectionOid.
     * @throws FindException   if the connection pointed to by the endpoint cannot be loaded
     */
    @Transactional(readOnly=true)
    void testEndpoint(JmsConnection connection, JmsEndpoint endpoint) throws JmsTestException, FindException;
}
