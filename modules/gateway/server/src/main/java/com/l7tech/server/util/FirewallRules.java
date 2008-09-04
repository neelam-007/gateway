package com.l7tech.server.util;

import com.l7tech.common.io.PortRange;
import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.util.*;
import com.l7tech.gateway.common.transport.SsgConnector;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.NetworkInterface;
import java.net.InetAddress;

/**
 * Gathers information from firewall_rules files to build a map of ports in use cluster-wide.
 */
public class FirewallRules {
    protected static final Logger logger = Logger.getLogger(FirewallRules.class.getName());

    /**
     * Write the firewall rules to the specified path using the specified source data.
     *
     * @param pathToWrite  the path to the firewall_rules file to create or overwrite. Required.
     * @param clusterRmiPort  the cluster RMI port to include in the written-out firewall rules.
     * @param connectors  all SsgConnector instances to include in the written-out firewall rules.  May be empty but mustn't be null.
     * @throws java.io.IOException if there is a problem writing out the firewall rules file.
     */
    public static void writeFirewallDropfile(String pathToWrite, final int clusterRmiPort, final Collection<SsgConnector> connectors) throws IOException {
        FileUtils.saveFileSafely(pathToWrite,  new FileUtils.Saver() {
            public void doSave(FileOutputStream fos) throws IOException {
                writeFirewallRules(fos, clusterRmiPort, connectors);
            }
        });
    }

    /**
     * [0:0] -A INPUT -i INTERFACE -p tcp -m tcp --dport 22:23 -j ACCEPT
     */
    static void writeFirewallRules(OutputStream fos, int clusterRmiPort, Collection<SsgConnector> connectors) throws IOException
    {
        PrintStream ps = new PrintStream(fos);
        try {
            final ArrayList<SsgConnector> list = new ArrayList<SsgConnector>(connectors);

            // Add a pseudo-connector for the inter-node communication port
            SsgConnector rc = new SsgConnector();
            rc.setPort(clusterRmiPort);
            list.add(rc);

            for (SsgConnector connector : list) {
                String device = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
                String interfaceName = null;
                if ( InetAddressUtil.isValidIpAddress(device) ) {
                    interfaceName = getInterfaceForIP(device);
                    if ( interfaceName == null ) {
                        logger.log( Level.WARNING, "Could not determine interface for IP address ''{0}'', this connector will be inaccessible.", device);
                        continue; // fail closed
                    }
                }

                List<PortRange> ranges = connector.getTcpPortsUsed();
                for (PortRange range : ranges) {
                    int portStart = range.getPortStart();
                    int portEnd = range.getPortEnd();

                    ps.print("[0:0] -A INPUT ");
                    if ( interfaceName != null ) {
                        ps.print(" -i ");
                        ps.print(interfaceName);
                    }
                    ps.print(" -p tcp -m tcp --dport ");
                    if (portStart == portEnd)
                        ps.print(portStart);
                    else
                        ps.printf("%d:%d", portStart, portEnd);
                    ps.print(" -j ACCEPT");
                    ps.println();
                }
            }
            ps.flush();

            if (ps.checkError()) throw new IOException("Error while writing firewall rules");
        } finally {
            ps.flush();
        }
    }

    private static String getInterfaceForIP( final String address ) {
        String name = null;

        try {
            NetworkInterface ni;
            if ( InetAddress.getByName(address).isLoopbackAddress() ) {
                ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            } else {
                ni = NetworkInterface.getByInetAddress(InetAddress.getByName(address));
            }

            while ( ni.isVirtual() ) {
                ni = ni.getParent();
            }

            name = ni.getName();
        } catch ( IOException ioe ) {
            logger.log( Level.FINE, "Unable to determine network interface for ip '"+address+"'." , ExceptionUtils.getDebugException(ioe));
        }

        return name;
    }
}
