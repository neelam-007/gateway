package com.l7tech.common.io;

import java.util.Collection;
import java.util.List;
import java.util.Arrays;
import java.net.InetAddress;
import java.io.Serializable;

/**
 * Represents a range of TCP or UDP ports.
 */
public class PortRange implements Serializable, PortOwner {
    private final int portStart;
    private final int portEnd;
    private final boolean udp;
    private final InetAddress device;

    /**
     * Create a port range not confined to any particular device.
     *
     * @param portStart  the starting port, inclusive.
     * @param portEnd    the ending port, inclusive.  May be the same as the starting port.
     * @param udp        if true, this is a UDP port range.  Otherwise, it's TCP.
     * @throws IllegalArgumentException if either port is out of range.
     */
    public PortRange(int portStart, int portEnd, boolean udp) {
        this(portStart, portEnd, udp, null);
    }

    /**
     * Create a port range that uses the specified device.
     *
     * @param portStart  the starting port, inclusive.
     * @param portEnd    the ending port, inclusive.  May be the same as the starting port.
     * @param udp        if true, this is a UDP port range.  Otherwise, it's TCP.
     * @param device     a particular device this port exists on, or null for INADDR_ANY
     * @throws IllegalArgumentException if either port is out of range.
     */
    public PortRange(int portStart, int portEnd, boolean udp, InetAddress device) {
        if (portStart > portEnd) {
            int tmp = portEnd;
            portEnd = portStart;
            portStart = tmp;
        }
        if (portStart < 0 || portEnd > 65535) throw new IllegalArgumentException("Port range must be between 0 and 65535");
        this.portStart = portStart;
        this.portEnd = portEnd;
        this.udp = udp;
        this.device = device;
    }

    /** @return the lowest port in the range. */
    public int getPortStart() {
        return portStart;
    }

    /** @return the highest port in the range. */
    public int getPortEnd() {
        return portEnd;
    }

    /** @return true if this range represents a UDP port. */
    public boolean isUdp() {
        return udp;
    }

    /** @return the device associated with this port range, or null if not confined to any one in particular. */
    public InetAddress getDevice() {
        return device;
    }

    private static boolean devicesOverlap(InetAddress left, InetAddress right) {
        return right == null || left == null || left.isAnyLocalAddress() || right.isAnyLocalAddress() || left.equals(right);
    }

    public boolean isPortUsed(int port, boolean udp, InetAddress otherDevice) {
        return udp == this.udp &&
               devicesOverlap(getDevice(), otherDevice) &&
               port >= portStart && port <= portEnd;
    }

    public boolean isOverlapping(PortRange other) {
        return isUdp() == other.isUdp() &&
               devicesOverlap(getDevice(), other.getDevice()) &&
               getPortStart() <= other.getPortEnd() &&
               other.getPortStart() <= getPortEnd();
    }

    public List<PortRange> getUsedPorts() {
        return Arrays.asList(this);
    }

    /**
     * Check if the specified port is in use by any of the specified port owners.
     *
     * @param owners the owners to check.  Required.
     * @param port   the port to check.
     * @param udp    whether the port to check is a UDP port.
     * @param device the device to match, or null to match any device.
     * @return true if the specified port is in use in the specified collection of owners.
     */
    public static boolean isPortUsed(Collection<? extends PortOwner> owners, int port, boolean udp, InetAddress device) {
        for (PortOwner owner : owners) {
            if (owner.isPortUsed(port, udp, device))
                return true;
        }
        return false;
    }

    /**
     * Check if any of the specified port owners have claimed any port in the specified range.
     *
     * @param owners  a list of port owners to check.  Required.
     * @param range   a port range to check.  Required.
     * @return true iff. any port owner claims any port from range
     */
    public static boolean isOverlapping(Collection<? extends PortOwner> owners, PortRange range) {
        for (PortOwner owner : owners) {
            if (owner.isOverlapping(range))
                return true;
        }
        return false;
    }

    /**
     * Check if any of the specified port owners have claimed any port in any of the specified ranges.
     *
     * @param owners  a list of port owners to check.  Required.
     * @param ranges  a list of port ranges to check.  Required.
     * @return true iff. any port owner claims any port from any range
     */
    public static boolean isOverlapping(Collection<? extends PortOwner> owners, Collection<? extends PortRange> ranges) {
        for (PortRange range : ranges) {
            if (isOverlapping(owners, range))
                return true;
        }
        return false;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PortRange portRange = (PortRange)o;

        if (portEnd != portRange.portEnd) return false;
        if (portStart != portRange.portStart) return false;
        if (udp != portRange.udp) return false;
        //noinspection RedundantIfStatement
        if (device != null ? !device.equals(portRange.device) : portRange.device != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = portStart;
        result = 31 * result + portEnd;
        result = 31 * result + (udp ? 1 : 0);
        result = 31 * result + (device != null ? device.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "[PortRange " + (udp ? "UDP " : "TCP ") + (device == null ? "" : device + " ") + portStart + "-" + portEnd + "]"; 
    }
}
