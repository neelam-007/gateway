package com.l7tech.gateway.config.manager;

import com.l7tech.util.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intended to be invoked for software installs, where obtaining this information as non-root is not always reliable).
 *
 * Sets node.properties / node.cluster.mac to the first MAC address detected.
 * Sets node.properties / node.cluster.ip to the first IP address of the interface with the detected MAC address
 *
 * @author jbufu
 */
public class ClusterAddressConfig {

    private static final Logger logger = Logger.getLogger(ClusterAddressConfig.class.getName());

    private static final String DEFAULT_CONFIG_PATH = "../node/{0}/etc/conf";
    private static final String DEFAULT_NODE = "default";

    private static final String NODE = SyspropUtil.getString("com.l7tech.config.node", DEFAULT_NODE);
    private static final String CONFIG_PATH = SyspropUtil.getString("com.l7tech.config.path", DEFAULT_CONFIG_PATH);

    private static final String NODE_PROPS_FILE = "node.properties";
    private static final String NODEPROPERTIES_CLUSTER_MAC = "node.cluster.mac";
    private static final String NODEPROPERTIES_CLUSTER_IP = "node.cluster.ip";

    public static void main(final String[] args) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties");
        try {
            File nodePropsFile = new File( MessageFormat.format(CONFIG_PATH, NODE), NODE_PROPS_FILE );
            if (nodePropsFile.exists()) {
                PropertiesConfiguration props = new PropertiesConfiguration();
                logger.log( Level.INFO, "Loading node configuration from ''{0}''.", nodePropsFile.getAbsolutePath());
                FileInputStream origFis = null;
                try {
                    origFis = new FileInputStream( nodePropsFile );
                    props.load(origFis);
                } catch (ConfigurationException ce) {
                    throw new CausedIOException("Error reading properties file '"+nodePropsFile.getAbsolutePath()+"'.", ce);
                } catch (FileNotFoundException e) {
                    throw new CausedIOException("Error reading properties file '"+nodePropsFile.getAbsolutePath()+"'.", e);
                } finally {
                    ResourceUtils.closeQuietly(origFis);
                }

                boolean isAppliance = isAppliance();
                logger.info("IsAppliance: " + isAppliance);
                if (! isAppliance &&  ! props.containsKey(NODEPROPERTIES_CLUSTER_MAC) && ! props.containsKey(NODEPROPERTIES_CLUSTER_IP)) {
                    Pair<String, String> macAndIp = getMacAndIp();
                    if ( macAndIp.left != null && ! macAndIp.left.trim().isEmpty() && macAndIp.right != null && ! macAndIp.right.trim().isEmpty() ) {
                        logger.info("Cluster address MAC: " + macAndIp.left + " IP: " + macAndIp.right);
                        props.setProperty(NODEPROPERTIES_CLUSTER_MAC, macAndIp.left.trim());
                        props.setProperty(NODEPROPERTIES_CLUSTER_IP, macAndIp.right.trim());

                        FileOutputStream origFos = null;
                        try {
                            origFos = new FileOutputStream( nodePropsFile );
                            props.save(origFos);
                            nodePropsFile.setReadable(true, false);
                        } catch (ConfigurationException ce) {
                            throw new CausedIOException("Error writing properties file '"+nodePropsFile.getAbsolutePath()+"'.", ce);
                        } catch (FileNotFoundException e) {
                            throw new CausedIOException("Error writing properties file '"+nodePropsFile.getAbsolutePath()+"'.", e);
                        } finally {
                            ResourceUtils.closeQuietly(origFos);
                        }
                    }
                }
            } else {
                logger.log(Level.WARNING, "Node configuration does not exist yet, not performing cluster MAC configuration.");
            }
        } catch (Throwable e) {
            String msg = "Unable configure cluster with the node's MAC address:" + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            System.err.println(msg);
            System.exit(1);
        }
    }

    private static Pair<String,String> getMacAndIp() {
        String mac = null;
        String ifName = null;
        InetAddress ipForMac = null;
        InetAddress ipForIfName = null;

        try {
            // get first mac and interface name
            for ( NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces()) ) {
                byte[] macAddr = networkInterface.getHardwareAddress();
                if ( macAddr != null ) {
                    mac = InetAddressUtil.formatMac(macAddr);
                    ifName = networkInterface.getName();
                    break;
                }
            }

            // get the ip
            for ( NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces()) ) {
                byte[] macAddr = networkInterface.getHardwareAddress();
                if ( macAddr != null && InetAddressUtil.formatMac(macAddr).equalsIgnoreCase(mac)) {
                    ipForMac = getFirstIp(networkInterface);
                    if (ipForMac != null && ! ipForMac.isLinkLocalAddress()) break;
                } else if (networkInterface.getName().startsWith(ifName)) {
                    ipForIfName = getFirstIp(networkInterface);
                }
            }
        } catch (SocketException e) {
            logger.log( Level.FINE, "Error getting network interfaces '" + e.getMessage() + "'.", ExceptionUtils.getDebugException(e));
        }

        // strip %interfaceNumber InetAddress's string format (for ipv6)
        try {
            if (ipForMac != null) {
                ipForMac = InetAddress.getByAddress(ipForMac.getAddress());
            }
            if (ipForIfName != null) {
                ipForIfName = InetAddress.getByAddress(ipForIfName.getAddress());
            }
        } catch (UnknownHostException e) {
            // won't happen
        }

        return new Pair<String, String>(mac, 
            ipForMac != null && ! ipForMac.isLinkLocalAddress() ? ipForMac.getHostAddress() : // first preference for global address from the interface with mac
            ipForIfName != null ?  ipForIfName.getHostAddress() : // second preference for sub-interface address
            ipForMac != null ? ipForMac.getHostAddress() : null   // last preference for link-local address
        );
    }

    private static InetAddress getFirstIp(NetworkInterface networkInterface) {
        for (InetAddress addr : Collections.list(networkInterface.getInetAddresses())) {
            return addr;
        }
        return null;
    }

    private static boolean isAppliance() {
        // same as PCUtils.isAppliance(), not adding dependency here
        try {
            File applianceDir = new File("/opt/SecureSpan/Appliance");
            return applianceDir.exists() && applianceDir.isDirectory();
        } catch (Exception e) {
            logger.log(Level.INFO, "Error encountered while trying to determine if the host is an appliance; assuming it's a software install. " + ExceptionUtils.getMessage(e));
            return false;
        }
    }
}
