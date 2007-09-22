package com.l7tech.common.transport;

import static com.l7tech.common.security.rbac.EntityType.SSG_CONNECTOR;
import static com.l7tech.common.security.rbac.MethodStereotype.*;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;

/**
 * Provides a remote interface for creating, reading, updating and deleting
 * Gateway HTTP/HTTPS/FTP/FTPS listen ports.
 *
 * @see SsgConnector
 * @see EntityHeader
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=SSG_CONNECTOR)
public interface TransportAdmin {

    /**
     * Download all SsgConnector records on this cluster.
     *
     * @return a List of SsgConnector instances.  Never null.  Normally will always contain at least
     *         one SsgConnector header, for the connection over which this call is made.
     * @throws java.rmi.RemoteException on remote communication error
     * @throws FindException if there is a problem reading from the database
     */
    @Transactional(readOnly=true)
    @Secured(types=SSG_CONNECTOR, stereotype=FIND_ENTITIES)
    Collection<SsgConnector> findAllSsgConnectors() throws RemoteException, FindException;

    /**
     * Download a specific SsgConnector instance identified by its primary key.
     *
     * @param oid the object ID of the SsgConnector instance to download.  Required.
     * @return the requested SsgConnector instance.  Never null.
     * @throws java.rmi.RemoteException on remote communication error
     * @throws FindException if no SsgConnector is found on this cluster with the specified oid, or
     *                       if there is a problem reading from the database
     */
    @Transactional(readOnly=true)
    @Secured(types=SSG_CONNECTOR, stereotype=FIND_BY_PRIMARY_KEY)
    SsgConnector findSsgConnectorByPrimaryKey(long oid) throws RemoteException, FindException;

    /**
     * Store the specified new or existing SsgConnector. If the specified {@link SsgConnector} contains a
     * unique object ID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     * <p/>
     * You will not be permitted to save changes to the SsgConnector that owns the admin connection
     * you are currently using to access this API.
     *
     * @param connector  the connector to save.  Required.
     * @return the unique object ID that was updated or created.
     * @throws java.rmi.RemoteException on remote communication error
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the specified SsgConnector owns the current admin connection, or
     *                         if the requested information could not be updated for some other reason
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    long saveSsgConnector(SsgConnector connector) throws RemoteException, SaveException, UpdateException;

    /**
     * Delete a specific SsgConnector instance identified by its primary key.
     * <p/>
     * You will not be permitted to delete the SsgConnector that owns
     * the admin connection you are currently using to access this API.
     *
     * @param oid the object ID of the SsgConnector instance to delete.  Required.
     * @throws java.rmi.RemoteException on remote communication error
     * @throws DeleteException if the specified SsgConnector owns the current admin connection, or
     *                         if there is some other problem deleting the object
     * @throws FindException if the object cannot be found
     */
    @Secured(stereotype=DELETE_BY_ID)
    void deleteSsgConnector(long oid) throws RemoteException, DeleteException, FindException;

    /**
     * Get the names of all cipher suites available on this system.
     *
     * @return the list of all cipher suites, ie { "TLS_RSA_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA" }
     * @throws java.rmi.RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    String[] getAllCipherSuiteNames() throws RemoteException;

    /**
     * Get the names of all cipher suites enabled by default on this system.
     *
     * @return the list of all cipher suites that are enabled by default,
     *         ie { "TLS_RSA_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA" }
     * @throws java.rmi.RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    String[] getDefaultCipherSuiteNames() throws RemoteException;
}
