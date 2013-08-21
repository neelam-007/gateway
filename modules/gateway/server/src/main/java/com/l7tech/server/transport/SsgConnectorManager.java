package com.l7tech.server.transport;

import com.l7tech.common.io.PortRanges;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;

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
     * Begin advertising that a transport module is available for the specified transport protocol.
     * <p/>
     * Note that all this does is make available to the SSM the information that the specified protocol
     * is supported by this Gateway.  There is no other effect -- in particular, the transport module is
     * itself responsible for listening for application events indicating that a connector has been added,
     * removed, changed, enabled, or disabled.
     *
     * @param transportDescriptor description of the custom transport that will be provided by the module.  Required.  Must have a non-empty scheme.
     * @param transportModule the TransportModule that will be handling the specified protocol.  Required.
     */
    void registerTransportProtocol(TransportDescriptor transportDescriptor, TransportModule transportModule);

    /**
     * Stop advertising a transport module's willingness to handle the specified protocol.
     *
     * @param scheme the name of the protocol that was registered, ie "l7.raw.tcp".  Required.
     * @return the transport module that was previously handling the specified protocol, or null.
     */
    TransportModule unregisterTransportProtocol(String scheme);

    /**
     * Get the listen port transport protocols that currently have registered transport modules on this Gateway.
     *
     * @return an array of protocols, ie { "http", "https", "ftp", "ftps", "l7.raw.tcp", "l7.raw.udp" }.  Never null.
     */
    TransportDescriptor[] getTransportProtocols();

    /**
     * Get the list of port ranges that are reserved for system use and that cannot be bound by SsgConnector instances.
     *
     * @return a Collection of PortRange instances covering ports that are reserved for system use.  Never null, but may be empty.
     */
    PortRanges getReservedPorts();
}
