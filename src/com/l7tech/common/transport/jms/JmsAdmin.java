package com.l7tech.common.transport.jms;

import com.l7tech.objectmodel.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.Serializable;

/**
 * Remote interface for managaging JMS connections and endpoints.
 *
 * @author alex
 * @version $Revision$
 */
public interface JmsAdmin extends Remote {
    public static final class JmsTuple implements Serializable {
        public JmsTuple( JmsConnection conn, JmsEndpoint end ) {
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

    JmsProvider[] getProviderList() throws RemoteException, FindException;

    /**
     * Finds all {@link JmsConnection}s in the database.
     *
     * @return an array of {@link JmsConnection}s
     * @throws RemoteException
     * @throws FindException
     */
    JmsConnection[] findAllConnections() throws RemoteException, FindException;

    /**
     * Finds all {@link JmsEndpoint}s in the database, wrapped in a {@link JmsTuple} that also
     * includes its associated {@link JmsConnection}.
     *
     * @return An array of {@link JmsTuple}s
     * @throws RemoteException
     * @throws FindException
     */
    JmsTuple[] findAllTuples() throws RemoteException, FindException;

    /**
     * Finds the {@link JmsConnection} with the given OID.
     * Returns a transient instance that are is enrolled in the Hibernate session.
     *
     * @return a transient {@link JmsConnection}
     * @throws RemoteException
     * @throws FindException
     */
    JmsConnection findConnectionByPrimaryKey( long oid ) throws RemoteException, FindException;
    JmsEndpoint findEndpointByPrimaryKey( long oid ) throws RemoteException, FindException;
    void setEndpointMessageSource( long oid, boolean isMessageSource ) throws RemoteException, FindException, UpdateException;

    /**
     * Save the specified JmsConnection, which may or may not have been newly created by the caller, to the database.
     *
     * @param connection the JmsConnection to save
     * @return the OID assigned to the saved JmsConnection.
     */
    long saveConnection( JmsConnection connection ) throws RemoteException, UpdateException, SaveException, VersionException;

    /**
     * Save the specified JmsEndpoint, which may or may not have been newly created by the caller, to the database.
     *
     * @param endpoint the JmsEndpoint to save
     * @return the OID assigned to the saved JmsEndpoint.
     */
    long saveEndpoint( JmsEndpoint endpoint ) throws RemoteException, UpdateException, SaveException, VersionException;

    void deleteEndpoint( long endpointOid ) throws RemoteException, FindException, DeleteException;
    void deleteConnection( long connectionOid ) throws RemoteException, FindException, DeleteException;

    /**
     * Returns an array of {@link JmsEndpoint}s that belong to the {@link JmsConnection} with the provided OID.
     *
     * Returns transient instances that are not enrolled in the Hibernate session.
     * @param connectionOid
     * @return an array of transient {@link JmsEndpoint}s
     * @throws RemoteException
     * @throws FindException
     */
    JmsEndpoint[] getEndpointsForConnection( long connectionOid ) throws RemoteException, FindException;

    /**
     * Test the specified JmsConnection, which may or may not exist in the database.  The Gateway will use the
     * specified settings to open a JMS connection.  If this succeeds, the caller can assume that the settings
     * are valid.
     *
     * @param connection  JmsConnection settings to test.  Might not yet have an OID.
     * @throws RemoteException
     */
    void testConnection(JmsConnection connection) throws RemoteException, JmsTestException;

    /**
     * Test the specified JmsEndpoint on the specified JmsConnection, either or both of which may or may not exist in
     * the database.  The Gateway will use the specified settings to open a JMS
     * connection and attempt to verify the existence of a Destination for this JmsEndpoint.
     *
     * @param connection JmsConnection settings to test.  Might not yet have an OID.
     * @param endpoint JmsEndpoint settings to test.  Might not yet have an OID or a valid connectionOid.
     * @throws FindException if the connection pointed to by the endpoint cannot be loaded
     * @throws RemoteException
     */
    void testEndpoint(JmsConnection connection, JmsEndpoint endpoint) throws RemoteException, JmsTestException, FindException;
}
