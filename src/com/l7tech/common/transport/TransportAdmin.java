package com.l7tech.common.transport;

import com.l7tech.common.io.PortRange;
import static com.l7tech.common.security.rbac.EntityType.SSG_CONNECTOR;
import static com.l7tech.common.security.rbac.MethodStereotype.*;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.util.Pair;
import com.l7tech.common.util.Triple;
import com.l7tech.objectmodel.*;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.util.Collection;

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
     * @throws FindException if there is a problem reading from the database
     */
    @Transactional(readOnly=true)
    @Secured(types=SSG_CONNECTOR, stereotype=FIND_ENTITIES)
    Collection<SsgConnector> findAllSsgConnectors() throws FindException;

    /**
     * Download a specific SsgConnector instance identified by its primary key.
     *
     * @param oid the object ID of the SsgConnector instance to download.  Required.
     * @return the requested SsgConnector instance.  Never null.
     * @throws FindException if no SsgConnector is found on this cluster with the specified oid, or
     *                       if there is a problem reading from the database
     */
    @Transactional(readOnly=true)
    @Secured(types=SSG_CONNECTOR, stereotype=FIND_BY_PRIMARY_KEY)
    SsgConnector findSsgConnectorByPrimaryKey(long oid) throws FindException;

    /**
     * Exception thrown when an attempt is made to update or delete the admin connection
     * over which the update or delete request itself arrived.
     */
    public static class CurrentAdminConnectionException extends Exception {
        public CurrentAdminConnectionException(String message) {
            super(message);
        }
    }

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
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated for some other reason
     * @throws CurrentAdminConnectionException if the specified SsgConnector owns the current admin connection
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    long saveSsgConnector(SsgConnector connector) throws SaveException, UpdateException, CurrentAdminConnectionException;

    /**
     * Delete a specific SsgConnector instance identified by its primary key.
     * <p/>
     * You will not be permitted to delete the SsgConnector that owns
     * the admin connection you are currently using to access this API.
     *
     * @param oid the object ID of the SsgConnector instance to delete.  Required.
     * @throws DeleteException if there is some other problem deleting the object
     * @throws FindException if the object cannot be found
     * @throws CurrentAdminConnectionException if the specified SsgConnector owns the current admin connection
     */
    @Secured(stereotype=DELETE_BY_ID)
    void deleteSsgConnector(long oid) throws DeleteException, FindException, CurrentAdminConnectionException;

    /**
     * Get the names of all cipher suites available on this system.
     *
     * @return the list of all cipher suites, ie { "TLS_RSA_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA" }
     */
    @Transactional(readOnly=true)
    String[] getAllCipherSuiteNames();

    /**
     * Get the names of all cipher suites enabled by default on this system.
     *
     * @return the list of all cipher suites that are enabled by default,
     *         ie { "TLS_RSA_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA" }
     */
    @Transactional(readOnly=true)
    String[] getDefaultCipherSuiteNames();

    /**
     * Get all bindable internet addresses available on this Gateway node, if possible.
     *
     * @return an array of InetAddress instances, or null if this information is unavailable.
     */
    @Transactional(readOnly=true)
    InetAddress[] getAvailableBindAddresses();

    /**
     * Scan for connectors that conflict with ports in use on any other partition on this node.
     *
     * @return zero or more 3-tuples, one per conflict detected, listing: the OID of the SsgConnector
     *         in which the conflict was found; which of its port ranges was the problem; and, the name of
     *         the partition with which we are in conflict.
     *         <p/>
     *         The name will be null if the conflict is within our own partition.
     *         <p/>
     *         If no conflicts are detected this array will be empty.  This method never returns null.
     * @throws com.l7tech.objectmodel.FindException if there is a problem finding our connectors
     */
    @Transactional(readOnly=true)
    public Collection<Triple<Long, PortRange, String>> findAllPortConflicts() throws FindException;

    /**
     * Check whether the specified SsgConnector would conflict with any port currently known to be in
     * use by any partition on this node.
     *
     * @param unsavedConnector the connector to vet.  Required.
     * @return zero or more pairs, one per conflict detected,
     *         listing which of unsavedConnector's port ranges was the problem, and the name of
     *         the partition with which we are in conflict.
     *         <p/>
     *         The name will be null if the conflict is within our own partition.
     *         <p/>
     *         If no conflicts are detected this array will be empty.  This method never returns null.
     * @throws com.l7tech.objectmodel.FindException if there is a problem finding our connectors
     */
    @Transactional(readOnly=true)
    public Collection<Pair<PortRange, String>> findPortConflicts(SsgConnector unsavedConnector) throws FindException;

}
