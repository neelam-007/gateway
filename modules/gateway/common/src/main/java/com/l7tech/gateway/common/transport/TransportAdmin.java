package com.l7tech.gateway.common.transport;

import com.l7tech.common.io.PortRanges;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.*;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Provides a remote interface for creating, reading, updating and deleting Gateway HTTP/HTTPS/FTP/FTPS listen ports.
 *
 * @see SsgConnector
 * @see EntityHeader
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types={SSG_CONNECTOR})
@Administrative
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
    @Secured(types=SSG_CONNECTOR, stereotype=FIND_ENTITY)
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
     * Finds a particular {@link SsgActiveConnector} with the specified OID, or null if no such connector can be found.
     *
     * @param oid the OID of the SsgActiveConnector to retrieve
     * @return the SsgActiveConnector with the specified OID, or null if no such connector can be found.
     * @throws FindException If there was a problem accessing the requested information.
     */
    @Secured(stereotype=FIND_ENTITY, types=SSG_ACTIVE_CONNECTOR)
    @Transactional(readOnly=true)
    SsgActiveConnector findSsgActiveConnectorByPrimaryKey( long oid ) throws FindException;

    /**
     * Finds a particular {@link SsgActiveConnector} with the specified type and connector name, or null if no such connector can be found.
     *
     * @param type The active connector type
     * @param name The active connector name
     * @return the SsgActiveConnector with the specified type and connector name, or null if no such connector can be found.
     * @throws FindException If there was a problem accessing the requested information.
     */
    @Secured(stereotype=FIND_ENTITY, types=SSG_ACTIVE_CONNECTOR)
    @Transactional(readOnly=true)
    SsgActiveConnector findSsgActiveConnectorByTypeAndName( String type, String name ) throws FindException;

    /**
     * Retrieve all active connectors of the given type.
     *
     * @param type The active connector type
     * @return a Collection of SsgActiveConnector instances.  Never null.
     * @throws FindException If there was a problem accessing the requested information.
     */
    @Secured(stereotype=FIND_HEADERS, types=SSG_ACTIVE_CONNECTOR)
    @Transactional(readOnly=true)
    Collection<SsgActiveConnector> findSsgActiveConnectorsByType( String type ) throws FindException;

    /**
     * Store the specified new or updated SsgActiveConnector.
     *
     * <p>If the specified {@link SsgActiveConnector} contains a unique object
     * ID that already exists, this will replace the objects current
     * configuration with the new configuration. Otherwise, a new object will
     * be created.</p>
     *
     * @param activeConnector  the active connector to save.  Required.
     * @return the unique object ID that was updated or created.
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated
     */
    @Secured(stereotype=SAVE_OR_UPDATE, types=SSG_ACTIVE_CONNECTOR)
    long saveSsgActiveConnector( SsgActiveConnector activeConnector ) throws SaveException, UpdateException;

    /**
     * Delete the active connector for the given primary key.
     *
     * @param oid the object ID of the active connector to delete.  Required.
     * @throws DeleteException if there is some other problem deleting the object
     * @throws FindException if the object cannot be found
     */
    @Secured(stereotype=DELETE_BY_ID, types=SSG_ACTIVE_CONNECTOR)
    void deleteSsgActiveConnector( long oid ) throws DeleteException, FindException;

    /**
     * Get the names of all protocols supported by the default TLS provider on the current node.
     *
     * @param defaultProviderOnly if true, only those protocols supported by the default TLS provider will be included.  If false, all protocols supported by any known provider will be included.
     * @return an array of protocol names, ie { "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2" }.  Never null but may (theoretically) be empty.
     */
    @Transactional(readOnly=true)
    String[] getAllProtocolVersions(boolean defaultProviderOnly);

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
     * Get all protocols that have registered as handlers for SsgConnector-type listen ports.
     * Only handlers that are currently licensed will be included.
     *
     * @return an array of protocol descriptors, ie { "http", "https", "ftp", "ftps", "svn+ssh", "itms", "l7.raw.tcp" }.  Never null.
     */
    @Transactional(readOnly=true)
    TransportDescriptor[] getModularConnectorInfo();

    /**
     * Get the list of port ranges that are reserved for system use and that cannot be bound by SsgConnector instances.
     *
     * @return a Collection of PortRange instances covering ports that are reserved for system use.  Never null, but may be empty.
     */
    @Transactional(readOnly=true)
    PortRanges getReservedPorts();

    /**
     * @return true if IPv6 is enabled on the gateway
     */
    boolean isUseIpv6();

    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITY,types=RESOLUTION_CONFIGURATION)
    ResolutionConfiguration getResolutionConfigurationByName( final String name ) throws FindException;

    @Secured(stereotype=SAVE_OR_UPDATE,types=RESOLUTION_CONFIGURATION)
    long saveResolutionConfiguration( ResolutionConfiguration configuration ) throws SaveException;

    /**
     * Gets the xml part max bytes value set in the io.xmlPartMaxBytes cluster property
     * @return the xml part max bytes value set in the io.xmlPartMaxBytes cluster property
     */
    @Transactional(readOnly=true)
    long getXmlMaxBytes();


    /**
     * Check if SNMP Query built-in service is enabled or not.
     * @return true if SNMP Query built-in service is enabled.
     */
    @Transactional(readOnly=true)
    boolean isSnmpQueryEnabled();

    /**
     * Retrieve all the available firewall rules in the system.  This method require the user to have the {@link com.l7tech.gateway.common.security.rbac.MethodStereotype#FIND_ENTITIES} permission.
     * @return a {@link Collection} of configured firewall rules.
     * @throws FindException if it is unable to retrieve a list of configured firewall rules.
     */
    @Transactional(readOnly = true)
    @Secured(types=FIREWALL_RULE, stereotype=FIND_ENTITIES)
    Collection<SsgFirewallRule> findAllFirewallRules() throws FindException;

    /**
     * Remove a firewall rule from the system with the given oid.  This method require the user to have the {@link com.l7tech.gateway.common.security.rbac.MethodStereotype#DELETE_BY_ID} permission.
     * @param oid the rule oid to delete
     * @throws DeleteException if it is unable to delete the rule.
     * @throws FindException if it is unable to locate the rule to be deleted.
     * @throws CurrentAdminConnectionException if the specified {@link SsgFirewallRule} owns the current admin connection.
     */
    @Secured(types=FIREWALL_RULE, stereotype=DELETE_BY_ID)
    void deleteFirewallRule(long oid) throws DeleteException, FindException, CurrentAdminConnectionException;

    /**
     * Save a firewall rule to the system.  This method require the user to have the {@link com.l7tech.gateway.common.security.rbac.MethodStereotype#SAVE_OR_UPDATE} permission.
     * @param firewallRule the firewall rule to save.
     * @return the oid of the newly saved firewall rule or the oid an existing rule that was updated.
     * @throws SaveException if the rule can not be saved.
     * @throws UpdateException if the rule can not be updated
     * @throws CurrentAdminConnectionException if the specified {@link SsgFirewallRule} owns the current admin connection.
     */
    @Secured(types=FIREWALL_RULE, stereotype=SAVE_OR_UPDATE)
    long saveFirewallRule(final SsgFirewallRule firewallRule) throws SaveException, UpdateException, CurrentAdminConnectionException;
}