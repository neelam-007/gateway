package com.l7tech.common.transport.jms;

import com.l7tech.objectmodel.*;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for managaging JMS connections and endpoints.
 *
 * @author alex
 * @version $Revision$
 */
public interface JmsAdmin extends Remote {
    JmsProvider[] getProviderList() throws RemoteException, FindException;
    JmsConnection[] findAllConnections() throws RemoteException, FindException;
    JmsConnection findConnectionByPrimaryKey( long oid ) throws RemoteException, FindException;

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

    JmsEndpoint[] getEndpointsForConnection( long connectionOid ) throws RemoteException, FindException;
}
