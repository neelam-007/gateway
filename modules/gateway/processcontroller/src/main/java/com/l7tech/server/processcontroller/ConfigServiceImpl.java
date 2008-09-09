/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.config.OSDetector;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.host.IpAddressConfig;
import com.l7tech.server.management.config.host.PCHostConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.ResourceUtils;

import javax.ejb.TransactionAttribute;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Top-level (possibly only) DAO for management of Process Controller configuration entities
 * @author alex
 */
@TransactionAttribute
public class ConfigServiceImpl implements ConfigService {
    private static final Logger logger = Logger.getLogger(ConfigServiceImpl.class.getName());

    private final File processControllerHomeDirectory;
    private final File nodeBaseDirectory;
    private final HostConfig host;

    public ConfigServiceImpl() {
        String s = System.getProperty("com.l7tech.server.processcontroller.homeDirectory");
        if (s == null) {
            processControllerHomeDirectory = new File(System.getProperty("user.dir"));
            try {
                logger.info("Assuming Process Controller home directory is " + processControllerHomeDirectory.getCanonicalPath());
            } catch (IOException e) {
                throw new RuntimeException(e); // Can't happen
            }
        } else {
            processControllerHomeDirectory = new File(s);
        }

        s = System.getProperty("com.l7tech.server.processcontroller.nodeBaseDirectory");
        if (s == null) {
            File parent = processControllerHomeDirectory.getParentFile();
            nodeBaseDirectory = new File(parent, "Nodes");
        } else {
            nodeBaseDirectory = new File(s);
        }

        final File hostPropsFile = new File(getProcessControllerHomeDirectory(), "etc/host.properties");
        if (!hostPropsFile.exists())
            throw new IllegalStateException("Couldn't find " + hostPropsFile.getAbsolutePath());

        final FileInputStream is;
        final Properties hostProps;
        try {
            is = new FileInputStream(hostPropsFile);
            hostProps = new Properties();
            hostProps.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load " + hostPropsFile.getAbsolutePath(), e);
        }

        PCHostConfig config = new PCHostConfig();
        config.setGuid(getHostId(hostProps));
        config.setLocalHostname(getLocalHostname(hostProps));
        config.setHostType(getHostType(hostProps));
        if (OSDetector.isLinux()) {
            config.setOsType(HostConfig.OSType.RHEL);
        } else if (OSDetector.isSolaris()) {
            config.setOsType(HostConfig.OSType.SOLARIS);
        } else if (OSDetector.isWindows()) {
            config.setOsType(HostConfig.OSType.WINDOWS);
        } else {
            throw new IllegalStateException("Unsupported operating system"); // TODO muddle through?
        }

        reverseEngineerIps(config);
        reverseEngineerNodes(config);
        this.host = config;
    }

    public HostConfig getHost() {
        return host;
    }

    private HostConfig.HostType getHostType(Properties props) {
        String type = (String)props.get("host.type");
        if (type == null) {
            logger.info("host.type not set; assuming appliance");
            return HostConfig.HostType.APPLIANCE;
        } else try {
            return HostConfig.HostType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Unsupported host.type " + type + "; assuming \"software\"");
            return HostConfig.HostType.SOFTWARE;
        }
    }

    private String getHostId(Properties props) {
        String id = (String)props.get("host.id");
        if (id == null) throw new IllegalStateException("host.id not found");
        return id;
    }

    private String getLocalHostname(Properties hostProps) {
        String hostname = (String)hostProps.get("host.hostname");
        if (hostname != null) {
            logger.info("hostname is " + hostname);
            return hostname;
        }

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING, "Unable to get local hostname; using localhost", e);
            hostname = "localhost";
        }
        return hostname;
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

    public void addServiceNode(NodeConfig node) {
        host.getNodes().put(node.getName(), node);
    }

    public void updateServiceNode(NodeConfig node) {
        host.getNodes().put(node.getName(), node);
    }

    public File getNodeBaseDirectory() {
        return nodeBaseDirectory;
    }

    public File getProcessControllerHomeDirectory() {
        return processControllerHomeDirectory;
    }
}
