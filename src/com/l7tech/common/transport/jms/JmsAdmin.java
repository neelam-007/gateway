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
    EntityHeader[] findAllConnections() throws RemoteException, FindException;
    JmsConnection findConnectionByPrimaryKey( long oid ) throws RemoteException, FindException;
    long saveConnection( JmsConnection connection ) throws RemoteException, UpdateException, SaveException, VersionException;
    void deleteConnection( long connectionOid ) throws RemoteException, DeleteException;
}
