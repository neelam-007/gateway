package com.l7tech.common.io;

import com.l7tech.util.Pair;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a range of TCP or UDP ports.
 */
public class PortRange implements Serializable, PortOwner {
    private final int portStart;
    private final int portEnd;
    private final boolean udp;
    private final String device;

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
    public PortRange(int portStart, int portEnd, boolean udp, String device) {
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
    public String getDevice() {
        return device;
    }

    private static boolean devicesOverlap(String left, String right) {
        return right == null || left == null || left.equals(right);
    }

    @Override
    public boolean isPortUsed(int port, boolean udp, String otherDevice) {
        return udp == this.udp &&
               devicesOverlap(getDevice(), otherDevice) &&
               port >= portStart && port <= portEnd;
    }

    @Override
    public boolean isOverlapping(PortRange other) {
        return isUdp() == other.isUdp() &&
               devicesOverlap(getDevice(), other.getDevice()) &&
               getPortStart() <= other.getPortEnd() &&
               other.getPortStart() <= getPortEnd();
    }

    @Override
    public Pair<PortRange, PortRange> getFirstOverlappingPortRange(PortOwner owner) {
        for (PortRange range : owner.getUsedPorts()) {
            if (isOverlapping(range))
                return new Pair<PortRange, PortRange>(this, range);
        }
        return null;
    }

    /**
     * Utility method for checking for conflicts between a pair of PortOwner instances.
     * <p/>
     * Performance note: if one of the PortOwners is known to have an exceptionally fast (or constant) implementation
     * of {@link com.l7tech.common.io.PortOwner#getUsedPorts()}, pass it as thisOwner if possible, as it is the one
     * that is invoked in the inner loop.
     *
     * @param thisOwner  the left-hand owner, typically the proximate one.  Required.
     * @param otherOwner the right-hand owner, typically the distal one.  Required.
     * @return null if there are no conflicts; otherwise, a Pair describing the first conflict, with the left item being the range from the left-hand owner,
     *         and the right item being the conflicting range from the right-hand owner.
     */
    public static Pair<PortRange, PortRange> getFirstOverlappingPortRange(PortOwner thisOwner, PortOwner otherOwner) {
        for (PortRange otherRange : otherOwner.getUsedPorts()) {
            for (PortRange thisRange : thisOwner.getUsedPorts()) {
                if (thisRange.isOverlapping(otherRange))
                    return new Pair<PortRange, PortRange>(thisRange, otherRange);
            }
        }
        return null;
    }

    @Override
    public List<PortRange> getUsedPorts() {
        return Arrays.asList(this);
    }

    public boolean isSinglePort() {
        return portStart == portEnd;
    }

    @Override
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

    @Override
    public int hashCode() {
        int result;
        result = portStart;
        result = 31 * result + portEnd;
        result = 31 * result + (udp ? 1 : 0);
        result = 31 * result + (device != null ? device.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "[PortRange " + (udp ? "UDP " : "TCP ") + (device == null ? "" : device + " ") + portStart + "-" + portEnd + "]"; 
    }
}
