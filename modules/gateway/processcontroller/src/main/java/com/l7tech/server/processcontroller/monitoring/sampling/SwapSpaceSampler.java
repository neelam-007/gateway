/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;

import java.util.regex.Pattern;

/**
 *
 */
class SwapSpaceSampler extends HostPropertySampler<Long> {
    private static final Pattern SWAPFREE_MATCHER = Pattern.compile("(?m)^SwapFree:\\s*(\\d+)\\s*kB$", Pattern.MULTILINE);

    public SwapSpaceSampler(String componentId) {
        super(componentId, BuiltinMonitorables.SWAP_FREE_KIB.getName());
    }

    public Long sample() throws PropertySamplingException {
        return matchNumberFromFile("/proc/meminfo", SWAPFREE_MATCHER);
    }
}
