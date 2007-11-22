package com.l7tech.server.partition;

import com.l7tech.common.io.PortOwner;
import com.l7tech.common.io.PortRange;
import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.common.util.*;
import com.l7tech.common.transport.SsgConnector;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gathers information from firewall_rules files to build a map of ports in use cluster-wide.
 */
public class FirewallRules {
    protected static final Logger logger = Logger.getLogger(FirewallRules.class.getName());
    private static final String FIREWALL_RULES_FILENAME = "/appliance/firewall_rules";

    /**
     * Read the specified firewall rules and parse them into a list of port ranges.
     *
     * @param is the firewall rules to examine. Required.
     * @return a List of PortRange objects.  May be empty but never null.
     * @throws java.io.IOException if there is a problem reading or parsing the port range information.
     */
    static List<PortRange> parseFirewallRules(InputStream is) throws IOException {
        List<PortRange> ranges = new ArrayList<PortRange>();
        Pattern p = Pattern.compile(" -I INPUT \\$Rule_Insert_Point\\s*(?:-d (\\d+.\\d+.\\d+.\\d+))?\\s*-p tcp -m tcp --dport\\s*(\\d+)(?:\\:(\\d+))?\\s*-j ACCEPT");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                String device = m.group(1);
                int portStart = parseInt(m.group(2));
                String portEndStr = m.group(3);
                int portEnd = portEndStr == null ? portStart : parseInt(portEndStr);
                try {
                    PortRange range = new PortRange(portStart, portEnd, false, device == null ? null : InetAddress.getByName(device));
                    ranges.add(range);
                } catch (UnknownHostException uhe) {
                    logger.log(Level.WARNING, "Unable to resolve hostname in firewall rule: " + device + ": " + ExceptionUtils.getMessage(uhe), uhe);
                    // Add it as INADDR_ANY
                    PortRange range = new PortRange(portStart, portEnd, false);
                    ranges.add(range);
                }
            }
        }
        return ranges;
    }

    private static int parseInt(String s) throws IOException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            throw new IOException("Invalid number in config file", nfe);
        }
    }

    public static PartitionPortInfo getInfoForPartition(PartitionInformation pi) throws IOException {
        File rulesFile = new File(pi.getOSSpecificFunctions().getConfigurationBase() + "/firewall_rules");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(rulesFile);
            return new PartitionPortInfo(pi.getPartitionId(), parseFirewallRules(fis));
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
    }

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

                List<PortRange> ranges = connector.getTcpPortsUsed();
                for (PortRange range : ranges) {
                    int portStart = range.getPortStart();
                    int portEnd = range.getPortEnd();

                    ps.print("[0:0] -I INPUT $Rule_Insert_Point ");
                    if (InetAddressUtil.isValidIpAddress(device)) {
                        ps.print(" -d ");
                        ps.print(device);
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

    /**
     * Represents the ports in use on a single partition.
     */
    public static class PartitionPortInfo implements PortOwner {
        private final String partitionName;
        private final List<PortRange> portRanges;

        PartitionPortInfo(String partitionName, List<PortRange> portRanges) {
            this.partitionName = partitionName;
            this.portRanges = Collections.unmodifiableList(portRanges);
        }

        /** @return the partition name */
        public String getPartitionName() {
            return partitionName;
        }

        public boolean isPortUsed(int port, boolean udp, InetAddress device) {
            for (PortRange range : portRanges) {
                if (range.isPortUsed(port, udp, device))
                    return true;
            }
            return false;
        }

        public boolean isOverlapping(PortRange otherRange) {
            for (PortRange range : portRanges) {
                if (range.isOverlapping(otherRange))
                    return true;
            }
            return false;
        }

        public List<PortRange> getUsedPorts() {
            return portRanges;
        }

        public String toString() {
            return "[PartitionPortInfo part=" + getPartitionName() + " usedPorts=" + getUsedPorts() + "]";
        }
    }

    /**
     * Represents all ports in use by every partition found on the system.
     */
    public static class PortInfo implements PortOwner {
        private final List<PartitionPortInfo> parts;

        PortInfo(List<PartitionPortInfo> parts) {
            this.parts = Collections.unmodifiableList(parts);
        }

        public boolean isPortUsed(int port, boolean udp, InetAddress device) {
            return PortRange.isPortUsed(parts, port, udp, device);
        }

        public boolean isOverlapping(PortRange range) {
            return PortRange.isOverlapping(parts, Arrays.asList(range));
        }

        public List<PortRange> getUsedPorts() {
            List<PortRange> ret = new ArrayList<PortRange>();
            for (PartitionPortInfo part : parts) {
                ret.addAll(part.getUsedPorts());
            }
            return ret;
        }

        /**
         * Get the name of the first partition found that has a known port assignment that conflicts with
         * any of the specified port ranges.
         *
         * @param ranges the port ranges to check.  Required.
         * @param partitionNameToIgnore  name of a partition to ignore when checking for conflicts, or null to check every partition
         * @return a Pair of (name of a conflicting partition, PortRange that conflicted), or null if no conflict was detected.
         */
        public Pair<PortRange, String> findFirstConflict(Collection<? extends PortRange> ranges, String partitionNameToIgnore) {
            for (PartitionPortInfo part : parts) {
                if (part.getPartitionName().equals(partitionNameToIgnore))
                    continue;
                for (PortRange range : ranges) {
                    if (part.isOverlapping(range))
                        return new Pair<PortRange, String>(range, part.getPartitionName());
                }
            }
            return null;
        }

        /**
         * Get individual information about all partitions.
         *
         * @return a List of all PartitionPortInfo we have.  May be empty but never null.
         */
        public List<PartitionPortInfo> getAllPartitionInfo() {
            return parts;
        }

        public String toString() {
            return "[PortInfo: \n   " + HexUtils.join("\n   ", parts) + "\n]";
        }
    }

    /**
     * Get information about all TCP and UDP ports known to be in use by all partitions on this node.
     *
     * @return a PortInfo object containing the requested information.
     */
    public static PortInfo getAllInfo() {
        final PartitionManager partitionManager = PartitionManager.getInstance();
        partitionManager.enumeratePartitions(); // force it to get up-to-date info, since the Gateway process is long-running
        Set<String> partitionNames = partitionManager.getPartitionNames();
        List<PartitionPortInfo> ppis = new ArrayList<PartitionPortInfo>();
        for (String partitionName : partitionNames) {
            PartitionInformation pi = partitionManager.getPartition(partitionName);
            try {
                ppis.add(getInfoForPartition(pi));
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to read firewall information for partition " + partitionName + ": " + ExceptionUtils.getMessage(e));
            }
        }
        return new PortInfo(ppis);
    }
}
