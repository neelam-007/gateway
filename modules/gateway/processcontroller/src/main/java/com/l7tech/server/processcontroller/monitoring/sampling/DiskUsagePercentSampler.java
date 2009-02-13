/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 *
 */
class DiskUsagePercentSampler extends HostPropertySampler<Double> {
    private static final Pattern DF_MATCHER = Pattern.compile("(?m)(\\d+)\\s+\\d+\\s+(\\d+)\\s*%\\s+/\\s*$");
    private static final String DF_PATH = "/bin/df";

    public DiskUsagePercentSampler(String componentId) {
        super(componentId, BuiltinMonitorables.DISK_USAGE_PERCENT.getName());
    }

    public Double sample() throws PropertySamplingException {
        try {
            ProcResult result = ProcUtils.exec(new File(DF_PATH));
            long[] nums = matchNumbers(new String(result.getOutput()), "output from " + DF_PATH, DF_MATCHER, 2);
            return nums[1] / nums[0] * 100.0;
        } catch (IOException e) {
            throw new PropertySamplingException(e);
        }
    }
}