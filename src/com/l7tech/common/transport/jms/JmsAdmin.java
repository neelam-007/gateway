package com.l7tech.common.transport.jms;

import com.l7tech.objectmodel.*;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for managaging JMS providers.
 *
 * @author alex
 * @version $Revision$
 */
public interface JmsAdmin extends Remote {
    EntityHeader[] findAllProviders() throws RemoteException, FindException;
    JmsProvider findProviderByPrimaryKey( long oid ) throws RemoteException, FindException;
    long saveProvider( JmsProvider provider ) throws RemoteException, UpdateException, SaveException, VersionException;
    void deleteProvider( long providerOid ) throws RemoteException, DeleteException;
}
