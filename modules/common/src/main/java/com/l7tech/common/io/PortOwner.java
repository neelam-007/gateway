package com.l7tech.common.io;

import com.l7tech.util.Pair;

import java.util.List;

/**
 * Implemented by objects that own one or more TCP or UDP addresses.
 */
public interface PortOwner {
    /**
     * Check if a port is claimed by this PortOwner.
     *
     * @param port  the port to check
     * @param udp   true if port represents a UDP port
     * @param device a device to search within, or null to match any port on any device
     * @return  true if the specified port is in this range.
     */
     boolean isPortUsed(int port, boolean udp, String device);

    /**
     * Check if any port in the specified range is claimed by this PortOwner.
     *
     * @param range the range to check.  Required.
     * @return true if the specified range overlaps with this range.
     */
    boolean isOverlapping(PortRange range);

    /**
     * Check if any port in any of the specified PortOwner's ranges is claimed by this PortOwner.
     *
     * @param owner the other PortOwner to check against.  Required.
     * @return null if there is no overlap.  Otherwise, a Pair describing the first conflict we saw, with this PortOwner's range as the left item and the other PortOwner's range as the right.
     */
    Pair<PortRange, PortRange> getFirstOverlappingPortRange(PortOwner owner);

    /**
     * Get all ports claimed by this PortOwner.
     *
     * @return a List of PortRange instances.  May be empty but never null.
     */
    List<PortRange> getUsedPorts();
}
