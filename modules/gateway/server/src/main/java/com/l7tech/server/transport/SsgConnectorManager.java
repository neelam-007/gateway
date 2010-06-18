package com.l7tech.server.transport;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.gateway.common.transport.SsgConnector;

/**
 * Interface that provides the ability to do CRUD operations on SsgConnector rows in the database.
 */
public interface SsgConnectorManager extends EntityManager<SsgConnector, EntityHeader> {
    /**
     * Translate a bind address that references an interface tag into a concrete IP address.
     *
     * @param bindAddress either an IP address or the name of a configured interface tag, ie "vmnet8"
     * @param port TCP or UDP port number, for logging purposes.
     * @return dotted decimal IP for the corresponding local interface, ie "192.168.1.1"
     * @throws ListenerException if the bind address appears to be an interface tag but the tag cannot be matched to a local interface
     */
    String translateBindAddress(String bindAddress, int port) throws ListenerException;

    /**
     * Begin advertising that a transport module is available for the specified custom protocol.
     * <p/>
     * Note that all this does is make available to the SSM the information that the specified protocol
     * is supported by this Gateway.  There is no other effect -- in particular, the transport module is
     * itself responsible for listening for application events indicating that a connector has been added,
     * removed, changed, enabled, or disabled.
     *
     * @param protocolName the name of the protocol, ie "ftp". Required.
     * @param transportModule the TransportModule that will be handling the specified protocol.  Required.
     */
    void registerCustomProtocol(String protocolName, TransportModule transportModule);

    /**
     * Stop advertising a transport module's willingness to handle the specified protocol.
     *
     * @param protocolName the name of the protocol, ie "ftps".  Required.
     * @param transportModule the transport module that will no longer be handling the specified protocol.  Required.
     */
    void unregisterCustomProtocol(String protocolName, TransportModule transportModule);

    /**
     * Get the custom protocols that currently have registered transport modules on this Gateway.
     *
     * @return an array of protocols, ie { "http", "https", "ftp", "ftps", "l7.raw.tcp", "l7.raw.udp" }.  Never null.
     */
    String[] getCustomProtocols();
}
