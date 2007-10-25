package com.l7tech.common.io;

import java.util.Collection;

/**
 * Represents a range of TCP or UDP ports.
 */
public class PortRange {
    private final int portStart;
    private final int portEnd;
    private final boolean udp;
    private String device;

    /**
     * Create a port range.
     *
     * @param portStart  the starting port.
     * @param portEnd    the ending port.  May be the same as the starting port.
     * @param udp        if true, this is a UDP port range.  Otherwise, it's TCP.
     * @throws IllegalArgumentException if either port is out of range.
     */
    public PortRange(int portStart, int portEnd, boolean udp) {
        if (portStart > portEnd) {
            int tmp = portEnd;
            portEnd = portStart;
            portStart = tmp;
        }
        if (portStart < 0 || portEnd > 65535) throw new IllegalArgumentException("Port range must be between 0 and 65535");
        this.portStart = portStart;
        this.portEnd = portEnd;
        this.udp = udp;
    }

    public int getPortStart() {
        return portStart;
    }

    public int getPortEnd() {
        return portEnd;
    }

    public boolean isUdp() {
        return udp;
    }

    /**
     * Check if a port is within the port range.  Caller should also remember to check isUdp.
     *
     * @param port  the port to check
     * @return  true if the specified port is in this range.
     */
    public boolean isPortUsed(int port) {
        return port >= portStart && port <= portEnd;
    }

    /**
     * Check if this range overlaps with the specified range.
     *
     * @param other the other range to check.  Required.
     * @return true if the specified range overlaps with this range.
     */
    public boolean isOverlapping(PortRange other) {
        return isUdp() == other.isUdp() &&
               getPortStart() <= other.getPortEnd() &&
               other.getPortStart() <= getPortEnd();
    }

    /**
     * Check if the specified port is in use in any of the specified port ranges.
     *
     * @param ranges the ranges to check.  Required.
     * @param port   the port to check.
     * @param udp    whether the port to check is a UDP port.
     * @return true if the specified port is in use in the specified collection of ranges.
     */
    public static boolean isPortUsed(Collection<PortRange> ranges, int port, boolean udp) {
        for (PortRange range : ranges) {
            if (range.isUdp() == udp && range.isPortUsed(port))
                return true;
        }
        return false;
    }

    /**
     * Check if any port in one set of port ranges overlaps any port in the other set of port ranges.
     *
     * @param rangeA  a list of port ranges to check.  Required.
     * @param rangeB  a list of port ranges to check.  Required.
     * @return true iff. any port from any range in rangeA overlaps any port from any range in rangeB.
     */
    public static boolean isOverlapping(Collection<PortRange> rangeA, Collection<PortRange> rangeB) {
        for (PortRange range1 : rangeA) {
            for (PortRange range2 : rangeB) {
                if (range1.isOverlapping(range2))
                    return true;
            }
        }
        return false;
    }

    /**
     * Get the device associated with this port range.  The contents of this string are ignored by
     * this class.
     *
     * @return the device, or null if not set.
     */
    public String getDevice() {
        return device;
    }

    /**
     * Set a device for this port range.  The format and interpretation of this string are
     * not specified here and it is ignored by this class.
     *
     * @param device the device.  may be null
     */
    public void setDevice(String device) {
        this.device = device;
    }
}
