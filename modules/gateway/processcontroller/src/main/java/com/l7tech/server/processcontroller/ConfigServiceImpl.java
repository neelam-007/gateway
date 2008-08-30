/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.host.IpAddressConfig;
import com.l7tech.server.management.config.host.PCHostConfig;
import com.l7tech.server.management.config.node.*;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Top-level (possibly only) DAO for management of Process Controller configuration entities
 * @author alex
 */
@SuppressWarnings({ "JpaQlInspection" })
@TransactionAttribute
public class ConfigServiceImpl implements ConfigService {
    private static final Logger logger = Logger.getLogger(ConfigServiceImpl.class.getName());

    @PersistenceContext(properties = {})
    private EntityManager entityManager;

    private volatile HostConfig host;

    public HostConfig getHost() {
        if (host == null) {
            Query q = entityManager.createQuery("select g FROM PCHostConfig g");
            List<PCHostConfig> gs = q.getResultList();
            switch (gs.size()) {
                case 0:
                    logger.info("Creating new Gateway for localhost");
                    PCHostConfig g = new PCHostConfig();
                    g.setName("localhost");
                    g.setOsType(PCHostConfig.OSType.RHEL);
                    reverseEngineerIps(g);
                    reverseEngineerNodes(g);

                    entityManager.persist(g);
                    host = g;
                    break;
                case 1:
                    logger.info("Found Gateway in database");
                    host = gs.get(0);
                    break;
                default:
                    throw new IllegalStateException("Multiple Gateways found in the database");
            }
        }

        return host;
    }

    private void reverseEngineerNodes(PCHostConfig g) {
        final PartitionManager pmgr = PartitionManager.getInstance();
        for (String pid : pmgr.getPartitionNames()) {
            final PartitionInformation pinfo = pmgr.getPartition(pid);
            final PCNodeConfig node = new PCNodeConfig();
            node.setHost(g);
            node.setName(pid);
            node.getDatabaseUrlTemplate().put(DatabaseType.NODE_ALL, DatabaseConfig.Vendor.MYSQL.getUrlTemplate());
            final DatabaseConfig db = new DatabaseConfig();
            db.setHost("localhost");
            db.setNode(node);
            db.setName("ssg");
            db.setNodeUsername("gateway");
            db.setNodePassword("7layer");
            node.getDatabases().add(db);

            Collection<ConnectorConfig> conns = node.getConnectors();
            for (SsgConnector sc : pinfo.getConnectorsFromServerXml()) {
                ConnectorConfig cc = new ConnectorConfig();
                cc.setNode(node);
                cc.copyFrom(sc);
                conns.add(cc);
            }

            for (PartitionInformation.EndpointHolder endpointHolder : pinfo.getEndpoints()) {
                final String ip = endpointHolder.getIpAddress();
                final int port = endpointHolder.getPort();
                if (endpointHolder instanceof PartitionInformation.HttpEndpointHolder) {
                    PartitionInformation.HttpEndpointHolder httpEndpointHolder = (PartitionInformation.HttpEndpointHolder)endpointHolder;
                } else if (endpointHolder instanceof PartitionInformation.FtpEndpointHolder) {
                    PartitionInformation.FtpEndpointHolder o = (PartitionInformation.FtpEndpointHolder)endpointHolder;
                } else if (endpointHolder instanceof PartitionInformation.OtherEndpointHolder) {
                    PartitionInformation.OtherEndpointHolder otherEndpointHolder = (PartitionInformation.OtherEndpointHolder)endpointHolder;
                }
            }
            g.getNodes().put(node.getName(), node);
        }
    }

    private void reverseEngineerIps(PCHostConfig g) {
        final Set<IpAddressConfig> ips = g.getIpAddresses();

        final Enumeration<NetworkInterface> nifs;
        try {
            nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs != null && nifs.hasMoreElements()) {
                final NetworkInterface nif = nifs.nextElement();
                if (nif.isLoopback()) continue;
                final Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    final InetAddress addr = addrs.nextElement();
                    final IpAddressConfig iac = new IpAddressConfig(g);
                    iac.setHost(g);
                    iac.setInterfaceName(nif.getName());
                    iac.setIpAddress(addr.getHostAddress());
                    ips.add(iac);
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e); // Unlikely
        }
    }

    public void updateGateway(final PCHostConfig host) {
        this.host = entityManager.merge(host);
    }

    public void addServiceNode(NodeConfig node) {
        entityManager.persist(node);
        final PCHostConfig host = (PCHostConfig)getHost();
        host.getNodes().put(node.getName(), node);
        this.host = entityManager.merge(host);
    }

    public void updateServiceNode(NodeConfig node) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
