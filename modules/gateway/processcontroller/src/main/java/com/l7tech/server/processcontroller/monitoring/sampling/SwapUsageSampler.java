/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;

import java.util.regex.Pattern;

class SwapUsageSampler extends HostPropertySampler<Long> {
    private static final Pattern SWAPFREE_MATCHER = Pattern.compile("(?m)^SwapFree:\\s*(\\d+)\\s*kB$", Pattern.MULTILINE);
    private static final Pattern SWAPTOTAL_MATCHER = Pattern.compile("(?m)^SwapTotal:\\s*(\\d+)\\s*kB$", Pattern.MULTILINE);

    private static final String PROC_MEMINFO = "/proc/meminfo";

    public SwapUsageSampler(String componentId) {
        super(componentId, BuiltinMonitorables.SWAP_USAGE_KIB.getName());
    }

    public Long sample() throws PropertySamplingException {
        final String meminfo = readFile(PROC_MEMINFO);
        final long free = matchNumber(meminfo, PROC_MEMINFO, SWAPFREE_MATCHER);
        final long total = matchNumber(meminfo, PROC_MEMINFO, SWAPTOTAL_MATCHER);
        return total - free;
    }

}
