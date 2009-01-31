package com.l7tech.server.processcontroller.monitoring;

import java.io.File;
import java.util.regex.Pattern;

/**
 *
 */
public class RaidStatusSampler extends HostPropertySampler<RaidStatus> {
    private static final String PATH_MDSTAT = "/proc/mdstat";

    private static final Pattern ANY_ARRAYS_PRESENT_PATTERN =
            Pattern.compile("(?m)^\\s*\\d+ blocks[\\040-\\0176]+$");

    private static final Pattern ANY_DRIVE_NOT_UP_BUT_RECOVERING_PATTERN =
            Pattern.compile("(?m)^\\s*\\d+ blocks[\\040-\\0176]+ \\[(?!U+\\]$)[\\040-\\0176&&[^\\]]]+\\]$\\s*^[\\040-\\0176]+ recovery [\\040-\\0176]+$");

    private static final Pattern ANY_DRIVE_NOT_UP_AND_NOT_RECOVERING_PATTERN =
            Pattern.compile("(?m)^\\s*\\d+ blocks[\\040-\\0176]+ \\[(?!U+\\]$)[\\040-\\0176&&[^\\]]]+\\]$(?!\\s*^[\\040-\\0176]+ recovery [\\040-\\0176]+$)");

    public RaidStatusSampler(String componentId) {
        super(componentId, "raidStatus");
    }

    RaidStatus sample() throws PropertySamplingException {
        if (!new File(PATH_MDSTAT).exists())
            return RaidStatus.NOT_RAID;

        String mdstat = readFile(PATH_MDSTAT);
        
        if (!ANY_ARRAYS_PRESENT_PATTERN.matcher(mdstat).matches())
            return RaidStatus.NOT_RAID;

        if (ANY_DRIVE_NOT_UP_AND_NOT_RECOVERING_PATTERN.matcher(mdstat).matches())
            return RaidStatus.BAD;

        if (ANY_DRIVE_NOT_UP_BUT_RECOVERING_PATTERN.matcher(mdstat).matches())
            return RaidStatus.REBUILDING;

        return RaidStatus.OK;
    }
}
