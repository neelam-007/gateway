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
    JmsProvider[] getProviderList() throws RemoteException;
    EntityHeader[] findAllConnections() throws RemoteException, FindException;
    JmsConnection findConnectionByPrimaryKey( long oid ) throws RemoteException, FindException;

    /**
     * Save the specified JmsConnection, which may or may not have been newly created by the caller, to the database.
     *
     * @param connection the JmsConnection to save
     * @return the OID assigned to the saved JmsConnection.
     */
    long saveConnection( JmsConnection connection ) throws RemoteException, UpdateException, SaveException, VersionException;

    void deleteConnection( long connectionOid ) throws RemoteException, DeleteException;
}
