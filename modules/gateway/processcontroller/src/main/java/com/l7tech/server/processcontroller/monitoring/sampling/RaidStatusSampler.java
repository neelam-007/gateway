/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.api.monitoring.RaidStatus;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;

import java.io.File;
import java.util.regex.Pattern;

class RaidStatusSampler extends HostPropertySampler<RaidStatus> {
    private static final String PATH_MDSTAT = "/proc/mdstat";

    private static final Pattern ANY_ARRAYS_PRESENT_PATTERN =
            Pattern.compile("(?m)^\\s*\\d+ blocks[\\040-\\0176]+$");

    private static final Pattern ANY_DRIVE_NOT_UP_BUT_RECOVERING_PATTERN =
            Pattern.compile("(?m)^\\s*\\d+ blocks[\\040-\\0176]+ \\[(?!U+\\]$)[\\040-\\0176&&[^\\]]]+\\]$\\s*^[\\040-\\0176]+ recovery [\\040-\\0176]+$");

    private static final Pattern ANY_DRIVE_NOT_UP_AND_NOT_RECOVERING_PATTERN =
            Pattern.compile("(?m)^\\s*\\d+ blocks[\\040-\\0176]+ \\[(?!U+\\]$)[\\040-\\0176&&[^\\]]]+\\]$(?!\\s*^[\\040-\\0176]+ recovery [\\040-\\0176]+$)");

    public RaidStatusSampler(String componentId) {
        super(componentId, BuiltinMonitorables.RAID_STATUS.getName());
    }

    public RaidStatus sample() throws PropertySamplingException {
        if (!new File(PATH_MDSTAT).exists())
            return RaidStatus.NOT_RAID;

        String mdstat = readFile(PATH_MDSTAT);

        return parseOutput(mdstat);
    }

    static RaidStatus parseOutput(String mdstat) {
        if (!ANY_ARRAYS_PRESENT_PATTERN.matcher(mdstat).find())
            return RaidStatus.NOT_RAID;

        if (ANY_DRIVE_NOT_UP_AND_NOT_RECOVERING_PATTERN.matcher(mdstat).find())
            return RaidStatus.BAD;

        if (ANY_DRIVE_NOT_UP_BUT_RECOVERING_PATTERN.matcher(mdstat).find())
            return RaidStatus.REBUILDING;

        return RaidStatus.OK;
    }
}
