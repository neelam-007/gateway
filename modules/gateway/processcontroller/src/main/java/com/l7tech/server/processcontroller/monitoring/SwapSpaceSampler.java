package com.l7tech.server.processcontroller.monitoring;

import java.util.regex.Pattern;

/**
 *
 */
public class SwapSpaceSampler extends HostPropertySampler<Long> {
    private static final Pattern SWAPFREE_MATCHER = Pattern.compile("^SwapFree:\\s*(\\d+)\\s*kB$", Pattern.MULTILINE);

    public SwapSpaceSampler(String componentId) {
        super(componentId, "swapSpace");
    }

    Long sample() throws PropertySamplingException {
        return matchNumberFromFile("/proc/meminfo", SWAPFREE_MATCHER);
    }
}
