package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Triple;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;
import com.l7tech.common.io.PortRange;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.partition.FirewallRules;
import com.l7tech.server.tomcat.ConnectionIdValve;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.*;

/**
 * Server-side implementation of the TransportAdmin API.
 */
public class TransportAdminImpl implements TransportAdmin {
    private final ServerConfig serverConfig;
    private final SsgConnectorManager connectorManager;
    private final KeystoreUtils defaultKeystore;

    private FirewallRules.PortInfo portInfo;
    private long portInfoUpdated = 0;
    private Long portInfoCacheTime;

    public TransportAdminImpl(ServerConfig serverConfig, SsgConnectorManager connectorManager, KeystoreUtils defaultKeystore) {
        this.serverConfig = serverConfig;
        this.connectorManager = connectorManager;
        this.defaultKeystore = defaultKeystore;
    }

    public Collection<SsgConnector> findAllSsgConnectors() throws FindException {
        return connectorManager.findAll();
    }

    public SsgConnector findSsgConnectorByPrimaryKey(long oid) throws FindException {
        return connectorManager.findByPrimaryKey(oid);
    }

    /**
     * Check if the specified connector represents the current admin connection.
     *
     * @param oid  the oid of the connector to examine.
     * @return true if this apears to match the current thread's active admin connection
     */
    private boolean isCurrentAdminConnection(long oid) {
        long currentConnection = ConnectionIdValve.getConnectorOid();
        return oid == currentConnection;
    }

    public long saveSsgConnector(SsgConnector connector) throws SaveException, UpdateException, CurrentAdminConnectionException {
        if (isCurrentAdminConnection(connector.getOid()))
            throw new CurrentAdminConnectionException("Unable to modify connector for current admin connection");
        if (connector.getOid() == SsgConnector.DEFAULT_OID) {
            return connectorManager.save(connector);
        } else {
            connectorManager.update(connector);
            return connector.getOid();
        }
    }

    public void deleteSsgConnector(long oid) throws DeleteException, FindException, CurrentAdminConnectionException {
        if (isCurrentAdminConnection(oid))
            throw new CurrentAdminConnectionException("Unable to delete connector for current admin connection");
        connectorManager.delete(oid);
    }

    private SSLContext getSslContext(){
        SSLContext context;
        try {
            final KeyManager[] keyManagers;
            keyManagers = defaultKeystore.getSSLKeyManagers();
            context = SSLContext.getInstance("SSL");
            context.init(keyManagers, null, null);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(ExceptionUtils.getMessage(e));
        } catch (KeyManagementException e) {
            throw new RuntimeException(ExceptionUtils.getMessage(e));
        } catch (KeyStoreException e) {
            throw new RuntimeException(ExceptionUtils.getMessage(e));
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(ExceptionUtils.getMessage(e));
        }
        return context;
    }

    public String[] getAllCipherSuiteNames() {
        return getSslContext().getSupportedSSLParameters().getCipherSuites();
    }

    public String[] getDefaultCipherSuiteNames() {
        return getSslContext().getDefaultSSLParameters().getCipherSuites();
    }

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
            return ret.toArray(new InetAddress[0]);
        } catch (SocketException e) {
            throw new RuntimeException("Unable to get network interfaces: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public Collection<Triple<Long, PortRange, String>> findAllPortConflicts() throws FindException {
        String ourPartitionName = serverConfig.getPropertyCached(ServerConfig.PARAM_PARTITION_NAME);
        int clusterPort = serverConfig.getIntPropertyCached(ServerConfig.PARAM_CLUSTER_PORT, 2124, 5000L);
        FirewallRules.PortInfo portInfo = getPortInfo();
        Collection<SsgConnector> connectors = findAllEnabledSsgConnectors();
        Collection<Triple<Long, PortRange, String>> ret = new ArrayList<Triple<Long, PortRange, String>>();
        for (SsgConnector connector : connectors) {
            // Check against other connectors in our own partition
            PortRange conflictingRange = findFirstConflict(connectors, connector);
            if (conflictingRange != null)
                ret.add(new Triple<Long, PortRange, String>(connector.getOid(), conflictingRange, null));

            // Check against the cluster RMI port
            if (connector.isPortUsed(clusterPort, false, null))
                ret.add(new Triple<Long, PortRange, String>(connector.getOid(), new PortRange(clusterPort, clusterPort, false), null));

            // Check against other connectors in other partitions
            Pair<PortRange,String> conflict = portInfo.findFirstConflict(connector.getUsedPorts(), ourPartitionName);
            if (conflict != null) {
                String partName = conflict.right;
                if (ourPartitionName.equals(partName)) partName = null;
                ret.add(new Triple<Long, PortRange, String>(connector.getOid(), conflict.left, partName));
            }
        }

        return ret;
    }

    public Collection<Pair<PortRange, String>> findPortConflicts(SsgConnector connector) throws FindException {
        String ourPartitionName = serverConfig.getPropertyCached(ServerConfig.PARAM_PARTITION_NAME);
        int clusterPort = serverConfig.getIntPropertyCached(ServerConfig.PARAM_CLUSTER_PORT, 2124, 5000L);
        Collection<Pair<PortRange, String>> ret = new ArrayList<Pair<PortRange, String>>();

        // Check against other connectors in our own partition first
        Collection<SsgConnector> connectors = findAllEnabledSsgConnectors();
        PortRange conflictingRange = findFirstConflict(connectors, connector);
        if (conflictingRange != null)
            ret.add(new Pair<PortRange, String>(conflictingRange, null));

        // Check against the cluster RMI port
        if (connector.isPortUsed(clusterPort, false, null))
            ret.add(new Pair<PortRange, String>(new PortRange(clusterPort, clusterPort, false), null));        

        // Check against other partitions
        Pair<PortRange,String> conflict = getPortInfo().findFirstConflict(connector.getUsedPorts(), ourPartitionName);
        if (conflict != null) {
            String partName = conflict.right;
            if (ourPartitionName.equals(partName)) partName = null;
            ret.add(new Pair<PortRange, String>(conflict.left, partName));
        }

        return ret;
    }

    private Collection<SsgConnector> findAllEnabledSsgConnectors() throws FindException {
        List<SsgConnector> ret = new ArrayList<SsgConnector>();
        Collection<SsgConnector> got = connectorManager.findAll();
        for (SsgConnector connector : got) {
            if (connector.isEnabled())
                ret.add(connector);
        }
        return ret;
    }

    /**
     * Return the first conflicting port range found between connector and any connector in connectors.
     * <p/>
     * The connector will not be compared with itself, even if it (or a copy of it with the same OID)
     * appears in connectors.
     *
     * @param connectors a set of connectors to check against.  Required.
     * @param connector  the connector to check.  Required.
     * @return the first conflicting port range from connector that was found to conflict with a port
     *         in use by connectors, or null if no conflict was detected.
     */
    private PortRange findFirstConflict(Collection<SsgConnector> connectors, SsgConnector connector) {
        PortRange conflictingRange = null;
        for (SsgConnector otherConnector : connectors) {
            if (otherConnector == connector || otherConnector.getOid() == connector.getOid())
                continue;
            List<PortRange> ranges = connector.getUsedPorts();
            for (PortRange range : ranges) {
                if (otherConnector.isOverlapping(range)) {
                    conflictingRange = range;
                    break;
                }
            }
        }
        return conflictingRange;
    }

    private long getPortInfoCacheTime() {
        if (portInfoCacheTime == null)
            portInfoCacheTime = SyspropUtil.getLong("com.l7tech.server.partitions.portinfo.maxCacheMillis", 2200L);
        return portInfoCacheTime;
    }

    private synchronized FirewallRules.PortInfo getPortInfo() {
        if (portInfo != null && System.currentTimeMillis() - portInfoUpdated <= getPortInfoCacheTime())
            return portInfo;
        portInfoUpdated = System.currentTimeMillis();
        return portInfo = FirewallRules.getAllInfo();
    }
}
