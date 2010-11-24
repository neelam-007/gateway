package com.l7tech.server.util;

import com.l7tech.common.io.PortRange;
import com.l7tech.server.config.systemconfig.IpProtocol;
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
     * @param connectors  all SsgConnector instances to include in the written-out firewall rules.  May be empty but mustn't be null.
     * @param ipProtocol  determines which connectors' bind addresses will be used to write the firewall rules, based on their IP protocol (IPv4 or IPv6)
     * @throws java.io.IOException if there is a problem writing out the firewall rules file.
     */
    public static void writeFirewallDropfile(String pathToWrite, final Collection<SsgConnector> connectors, final IpProtocol ipProtocol) throws IOException {
        FileUtils.saveFileSafely(pathToWrite,  new FileUtils.Saver() {
            @Override
            public void doSave(FileOutputStream fos) throws IOException {
                writeFirewallRules(fos, connectors, ipProtocol);
            }
        });
    }

    /**
     * [0:0] -A INPUT -i INTERFACE -p tcp -m tcp --dport 22:23 -j ACCEPT
     */
    static void writeFirewallRules(OutputStream fos, Collection<SsgConnector> connectors, final IpProtocol ipProtocol) throws IOException
    {
        PrintStream ps = new PrintStream(fos);
        try {
            final ArrayList<SsgConnector> list = new ArrayList<SsgConnector>(connectors);

            for (SsgConnector connector : list) {
                String bindAddress = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
                String interfaceName = null;
                if ( bindAddress != null && ipProtocol.validateAddress(bindAddress).isEmpty() ) {
                    interfaceName = getInterfaceForIP(bindAddress);
                    if ( interfaceName == null ) {
                        logger.log( Level.WARNING, "Could not determine interface for IP address ''{0}'', this connector will be inaccessible.", bindAddress);
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

            if ( ni != null ) {
                while ( ni.isVirtual() ) {
                    ni = ni.getParent();
                }

                name = ni.getName();
            }
        } catch ( IOException ioe ) {
            logger.log( Level.FINE, "Unable to determine network interface for ip '"+address+"'." , ExceptionUtils.getDebugException(ioe));
        }

        return name;
    }
}
