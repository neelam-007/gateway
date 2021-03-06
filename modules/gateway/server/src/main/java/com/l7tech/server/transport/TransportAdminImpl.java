package com.l7tech.server.transport;

import com.l7tech.common.io.PortRanges;
import com.l7tech.gateway.common.transport.*;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.*;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.tomcat.ConnectionIdValve;
import com.l7tech.server.transport.firewall.SsgFirewallRuleManager;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * Server-side implementation of the TransportAdmin API.
 */
public class TransportAdminImpl implements TransportAdmin {
    private final SsgActiveConnectorManager ssgActiveConnectorManager;
    private final SsgConnectorManager connectorManager;
    private final ResolutionConfigurationManager resolutionConfigurationManager;
    private final Config config;
    private final SsgFirewallRuleManager firewallRuleManager;
    private final TransportAdminHelper transportAdminHelper;

    public TransportAdminImpl( final SsgActiveConnectorManager ssgActiveConnectorManager,
                               final SsgConnectorManager connectorManager,
                               final SsgFirewallRuleManager firewallRuleManager,
                               final ResolutionConfigurationManager resolutionConfigurationManager,
                               final DefaultKey defaultKeystore,
                               final Config config ) {
        this.ssgActiveConnectorManager = ssgActiveConnectorManager;
        this.connectorManager = connectorManager;
        this.resolutionConfigurationManager = resolutionConfigurationManager;
        this.config = config;
        this.firewallRuleManager = firewallRuleManager;
        this.transportAdminHelper = new TransportAdminHelper(defaultKeystore);
    }

    @Override
    public Collection<SsgConnector> findAllSsgConnectors() throws FindException {
        return connectorManager.findAll();
    }

    @Override
    public SsgConnector findSsgConnectorByPrimaryKey(Goid goid) throws FindException {
        return connectorManager.findByPrimaryKey(goid);
    }

    /**
     * Check if the specified connector represents the current admin connection.
     *
     * @param goid  the goid of the connector to examine.
     * @return true if this appears to match the current thread's active admin connection
     */
    private boolean isCurrentAdminConnection(Goid goid) {
        Goid currentConnection = ConnectionIdValve.getConnectorGoid();
        return goid.equals(currentConnection);
    }

    @Override
    public Goid saveSsgConnector(SsgConnector connector) throws SaveException, UpdateException, CurrentAdminConnectionException {
        if (isCurrentAdminConnection(connector.getGoid()))
            throw new CurrentAdminConnectionException("Unable to modify connector for current admin connection");
        if (connector.getGoid().equals(SsgConnector.DEFAULT_GOID)) {
            return connectorManager.save(connector);
        } else {
            connectorManager.update(connector);
            return connector.getGoid();
        }
    }

    @Override
    public void deleteSsgConnector(Goid goid) throws DeleteException, FindException, CurrentAdminConnectionException {
        if (isCurrentAdminConnection(goid))
            throw new CurrentAdminConnectionException("Unable to delete connector for current admin connection");
        connectorManager.delete(goid);
    }

    @Override
    public SsgActiveConnector findSsgActiveConnectorByPrimaryKey( final Goid goid ) throws FindException {
        return ssgActiveConnectorManager.findByPrimaryKey(goid);
    }

    @Override
    public SsgActiveConnector findSsgActiveConnectorByTypeAndName(String type, String name) throws FindException {
        if (type == null || name == null) return null;

        Collection<SsgActiveConnector> connectors = findSsgActiveConnectorsByType(type);
        for (SsgActiveConnector connector: connectors) {
            if (name.equals(connector.getName())) return connector;
        }

        return null;
    }

    @Override
    public Collection<SsgActiveConnector> findSsgActiveConnectorsByType( final String type ) throws FindException {
        return ssgActiveConnectorManager.findSsgActiveConnectorsByType(type);
    }

    @Override
    public Goid saveSsgActiveConnector( final SsgActiveConnector activeConnector ) throws SaveException, UpdateException {
        if ( activeConnector.getGoid().equals(SsgActiveConnector.DEFAULT_GOID)) {
            return ssgActiveConnectorManager.save( activeConnector );
        } else {
            ssgActiveConnectorManager.update( activeConnector );
            return activeConnector.getGoid();
        }
    }

    @Override
    public void deleteSsgActiveConnector( final Goid goid ) throws DeleteException, FindException {
        ssgActiveConnectorManager.delete(goid);
    }

    @Override
    public String[] getAllProtocolVersions(boolean defaultProviderOnly) {
        return transportAdminHelper.getAllProtocolVersions(defaultProviderOnly);
    }

    @Override
    public String[] getAllCipherSuiteNames() {
        return transportAdminHelper.getAllCipherSuiteNames();
    }

    @Override
    public String[] getDefaultCipherSuiteNames() {
        return transportAdminHelper.getDefaultCipherSuiteNames();
    }

    @Override
    public String[] getVisibleCipherSuiteNames() {
        return transportAdminHelper.getVisibleCipherSuiteNames();
    }

    @Override
    public InetAddress[] getAvailableBindAddresses() {
        try {
            List<InetAddress> ret = new ArrayList<InetAddress>();
            Enumeration<NetworkInterface> faces = NetworkInterface.getNetworkInterfaces();
            while (faces.hasMoreElements()) {
                NetworkInterface face = faces.nextElement();
                Enumeration<InetAddress> addrs = face.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    ret.add(addrs.nextElement());
                }
            }
            return ret.toArray(new InetAddress[ret.size()]);
        } catch (SocketException e) {
            throw new RuntimeException("Unable to get network interfaces: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public TransportDescriptor[] getModularConnectorInfo() {
        return connectorManager.getTransportProtocols();
    }

    @Override
    public PortRanges getReservedPorts() {
        return connectorManager.getReservedPorts();
    }

    @Override
    public boolean isUseIpv6() {
        return InetAddressUtil.isUseIpv6();
    }

    @Override
    public ResolutionConfiguration getResolutionConfigurationByName( final String name ) throws FindException {
        return resolutionConfigurationManager.findByUniqueName(name);
    }

    @Override
    public Goid saveResolutionConfiguration( final ResolutionConfiguration configuration ) throws SaveException {
        final Goid oid;
        if ( configuration.isUnsaved() ) {
            oid = resolutionConfigurationManager.save( configuration );
        } else {
            oid = configuration.getGoid();
            try {
                resolutionConfigurationManager.update( configuration );
            } catch ( UpdateException e ) {
                throw new SaveException( ExceptionUtils.getMessage(e), e );
            }
        }
        return oid;
    }

    @Override
    public long getXmlMaxBytes(){
        return Message.getMaxBytes();
    }

    @Override
    public boolean isSnmpQueryEnabled() {
        return config.getBooleanProperty(ServerConfigParams.PARAM_SNMP_QUERY_SERVICE_ENABLED, true);
    }

    @Override
    public Collection<SsgFirewallRule> findAllFirewallRules() throws FindException {
        return firewallRuleManager.findAll();
    }

    @Override
    public void deleteFirewallRule(final Goid goid) throws DeleteException, FindException, CurrentAdminConnectionException {
        firewallRuleManager.delete(goid);
    }

    @Override
    public Goid saveFirewallRule(final SsgFirewallRule firewallRule) throws SaveException, UpdateException, CurrentAdminConnectionException {
        if (isCurrentAdminConnection(firewallRule.getGoid()))
            throw new CurrentAdminConnectionException("Unable to modify connector for current admin connection");
        if (firewallRule.getGoid().equals(SsgFirewallRule.DEFAULT_GOID)) {
            return firewallRuleManager.save(firewallRule);
        } else {
            firewallRuleManager.update(firewallRule);
            return firewallRule.getGoid();
        }
    }
}