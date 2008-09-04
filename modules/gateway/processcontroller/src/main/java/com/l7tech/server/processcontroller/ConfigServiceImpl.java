/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.host.IpAddressConfig;
import com.l7tech.server.management.config.host.PCHostConfig;
import com.l7tech.server.management.config.node.*;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.util.ResourceUtils;

import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;

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

    @SuppressWarnings({"unchecked"})
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

                    host = entityManager.merge(g);
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
        try {
            File nodes = new File("../Nodes").getCanonicalFile();

            if ( nodes.isDirectory() ) {
                for ( File nodeDirectory : nodes.listFiles() ) {
                    File nodeConfigFile = new File( nodeDirectory, "etc/conf/node.properties" );
                    if ( !nodeConfigFile.isFile() ) continue;

                    Properties nodeProperties = new Properties();
                    InputStream in = null;
                    try {
                        nodeProperties.load(in = new FileInputStream(nodeConfigFile));
                    } finally {
                        ResourceUtils.closeQuietly(in);
                    }

                    if ( !nodeProperties.containsKey("node.enabled") ||
                         !nodeProperties.containsKey("node.id") ) {
                        logger.log( Level.WARNING, "Ignoring node ''{0}'' due to invalid properties.", nodeDirectory.getName());
                        continue;
                    }

                    final PCNodeConfig node = new PCNodeConfig();
                    node.setHost(g);
                    node.setName(nodeDirectory.getName());
                    node.setSoftwareVersion(SoftwareVersion.fromString("5.0")); //TODO get version for node
                    node.setEnabled( Boolean.valueOf(nodeProperties.getProperty("node.enabled")) );
                    node.setGuid( nodeProperties.getProperty("node.id") );

//                    final DatabaseConfig db = new DatabaseConfig();
//                    db.setType(DatabaseType.NODE_ALL);
//                    db.setHost( nodeProperties.getProperty() );
//                    db.setPort( Integer.parseInt(nodeProperties.getProperty()) );
//                    db.setName( nodeProperties.getProperty() );
//                    db.setNodeUsername( nodeProperties.getProperty() );
//                    db.setNodePassword( nodeProperties.getProperty() );
//                    node.getDatabases().add(db);

                    logger.log(Level.INFO, "Detected node ''{0}''.", nodeDirectory.getName());
                    g.getNodes().put(node.getName(), node);
                }
            }
        } catch (IOException ioe) {
            logger.log( Level.WARNING, "Error when detecting nodes.", ioe);
        } catch (NumberFormatException nfe) {
            logger.log( Level.WARNING, "Error when detecting nodes.", nfe);
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
        entityManager.merge(node);
        final PCHostConfig host = (PCHostConfig)getHost();
        host.getNodes().put(node.getName(), node);
        this.host = entityManager.merge(host);
    }

    public void updateServiceNode(NodeConfig node) {
        // TODO implement
    }

}
