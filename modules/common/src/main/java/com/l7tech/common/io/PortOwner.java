package com.l7tech.common.io;

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
     * Get all ports claimed by this PortOwner.
     *
     * @return a List of PortRange instances.  May be empty but never null.
     */
    List<PortRange> getUsedPorts();
}
