/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.management.config.gateway.GatewayConfig;
import com.l7tech.server.management.config.gateway.IpAddressConfig;
import com.l7tech.server.management.config.gateway.PCGatewayConfig;
import com.l7tech.server.management.config.node.*;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.net.Inet4Address;
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
@TransactionAttribute
public class ConfigServiceImpl implements ConfigService {
    private static final Logger logger = Logger.getLogger(ConfigServiceImpl.class.getName());

    @PersistenceContext(properties = {})
    private EntityManager entityManager;

    private volatile GatewayConfig gateway;

    public GatewayConfig getGateway() {
        if (gateway == null) {
            Query q = entityManager.createQuery("select g FROM PCGatewayConfig g");
            List<PCGatewayConfig> gs = q.getResultList();
            switch (gs.size()) {
                case 0:
                    logger.info("Creating new Gateway for localhost");
                    PCGatewayConfig g = new PCGatewayConfig();
                    g.setName("localhost");
                    g.setOsType(PCGatewayConfig.OSType.RHEL);
                    reverseEngineerIps(g);
                    reverseEngineerNodes(g);

                    entityManager.persist(g);
                    gateway = g;
                    break;
                case 1:
                    logger.info("Found Gateway in database");
                    gateway = gs.get(0);
                    break;
                default:
                    throw new IllegalStateException("Multiple Gateways found in the database");
            }
        }

        return gateway;
    }

    private void reverseEngineerNodes(PCGatewayConfig g) {
        final PartitionManager pmgr = PartitionManager.getInstance();
        for (String pid : pmgr.getPartitionNames()) {
            final PartitionInformation pinfo = pmgr.getPartition(pid);
            final PCServiceNodeConfig node = new PCServiceNodeConfig();
            node.setGateway(g);
            node.setName(pid);
            node.getDatabaseUrlTemplate().put(DatabaseType.GATEWAY_ALL, DatabaseConfig.Vendor.MYSQL.getUrlTemplate());
            final DatabaseConfig db = new DatabaseConfig();
            db.setHost("localhost");
            db.setNode(node);
            db.setName("ssg");
            db.setUsername("gateway");
            db.setServiceNodePassword("7layer");
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
            g.getServiceNodes().add(node);
        }
    }

    private void reverseEngineerIps(PCGatewayConfig g) {
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
                    iac.setGateway(g);
                    iac.setVersion(addr instanceof Inet4Address ? 4 : 6);
                    iac.setName(nif.getDisplayName());
                    iac.setInterfaceName(nif.getName());
                    iac.setIpAddress(addr.getHostAddress());
                    ips.add(iac);
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e); // Unlikely
        }
    }

    public void updateGateway(final PCGatewayConfig gateway) {
        this.gateway = entityManager.merge(gateway);
    }

    public void addServiceNode(ServiceNodeConfig node) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateServiceNode(ServiceNodeConfig node) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
