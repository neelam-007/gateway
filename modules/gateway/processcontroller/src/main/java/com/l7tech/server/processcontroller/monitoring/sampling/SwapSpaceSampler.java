/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import java.util.regex.Pattern;

/**
 *
 */
class SwapSpaceSampler extends HostPropertySampler<Long> {
    private static final Pattern SWAPFREE_MATCHER = Pattern.compile("^SwapFree:\\s*(\\d+)\\s*kB$", Pattern.MULTILINE);

    public SwapSpaceSampler(String componentId) {
        super(componentId, "swapSpace");
    }

    public Long sample() throws PropertySamplingException {
        return matchNumberFromFile("/proc/meminfo", SWAPFREE_MATCHER);
    }
}
