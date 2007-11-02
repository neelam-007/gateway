package com.l7tech.server.partition;

import com.l7tech.common.io.PortOwner;
import com.l7tech.common.io.PortRange;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;

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
public class FirewallRulesParser {
    protected static final Logger logger = Logger.getLogger(FirewallRulesParser.class.getName());

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
         * the specified port.
         *
         * @param port  the port to check
         * @param udp   true if port represents a UDP port
         * @param device a device to search within, or null to match any port on any device
         * @return the name of a conflicting partition, or null if no conflict was detected.
         */
        public String getNameOfFirstConflictingPartition(int port, boolean udp, InetAddress device) {
            for (PartitionPortInfo part : parts) {
                if (part.isPortUsed(port, udp, device))
                    return part.getPartitionName();
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
        Set<String> partitionNames = partitionManager.getPartitionNames();
        List<PartitionPortInfo> ppis = new ArrayList<PartitionPortInfo>();
        for (String partitionName : partitionNames) {
            PartitionInformation pi = partitionManager.getPartition(partitionName);
            try {
                ppis.add(FirewallRulesParser.getInfoForPartition(pi));
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to read firewall information for partition " + partitionName + ": " + ExceptionUtils.getMessage(e), e);
            }
        }
        return new PortInfo(ppis);
    }
}
