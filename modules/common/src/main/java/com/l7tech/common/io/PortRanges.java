package com.l7tech.common.io;

import com.l7tech.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a collection of zero or more {@link PortRange} instances.
 */
public class PortRanges implements Serializable, PortOwner {
    final List<PortRange> ranges;

    /**
     * Creates port ranges covering the specified ranges.
     *
     * @param ranges the ranges to cover, or null to represent an empty range.
     */
    public PortRanges(Collection<PortRange> ranges) {
        this.ranges = Collections.unmodifiableList(ranges != null ? new ArrayList<PortRange>(ranges) : Collections.<PortRange>emptyList());
    }

    @Override
    public boolean isPortUsed(int port, boolean udp, String device) {
        for (PortRange range : ranges) {
            if (range.isPortUsed(port, udp, device))
                return true;
        }
        return false;
    }

    @Override
    public boolean isOverlapping(PortRange range) {
        for (PortRange portRange : ranges) {
            if (portRange.isOverlapping(range))
                return true;
        }
        return false;
    }

    @Override
    public Pair<PortRange, PortRange> getFirstOverlappingPortRange(PortOwner owner) {
        return PortRange.getFirstOverlappingPortRange(this, owner);
    }

    @Override
    public List<PortRange> getUsedPorts() {
        return ranges;
    }
}
