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
     * Obtain the list of JMS endpoints that are monitored for incoming messages for processing by the SSG.
     *
     * @return the EntityHeaders of the JmsEndpoint instances that the SSG is monitoring.
     */
    EntityHeader[] findAllMonitoredEndpoints() throws RemoteException, FindException;

    /**
     * Replace the list of JMS endpoints that are monitored for incoming messages for processing by the SSG.
     * @throws RemoteException      in case of network trouble
     * @throws FindException        if any of the specified OIDs were not valid JmsEndpoints
     * @throws SaveException        if the changes could not be saved for some other reason 
     */
    void saveAllMonitoredEndpoints( long[] oids ) throws RemoteException, FindException, SaveException;

    /**
     * Save the specified JmsConnection, which may or may not have been newly created by the caller, to the database.
     *
     * @param connection the JmsConnection to save
     * @return the OID assigned to the saved JmsConnection.
     */
    long saveConnection( JmsConnection connection ) throws RemoteException, UpdateException, SaveException, VersionException;

    void deleteConnection( long connectionOid ) throws RemoteException, DeleteException;
}
