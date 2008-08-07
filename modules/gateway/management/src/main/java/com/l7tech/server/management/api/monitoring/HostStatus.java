/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.util.Pair;
import com.l7tech.util.Triple;

import java.util.List;
import java.util.Map;

/** @author alex */
public class HostStatus {
    /** Values are 1-, 5- and 15-minute averages */
    private Triple<Double, Double, Double> loadAverages;

    /** Values are temperatures, in degrees celsius, of each CPU in the Host, or an empty list if the information is not available. */
    private List<Double> cpuTemperatures;

    /** Percentage of CPU time spent in "user" state in the last five seconds */
    private double cpuSystem;

    /** Percentage of CPU time spent in "system" state in the last five seconds */
    private double cpuUser;

    /** Percentage of CPU time spent in "user" state in the last five seconds */
    private double cpuIdle;

    /** Percentage of CPU time spent in "idle" state in the last five seconds */
    private double cpuWait;

    /** Total, Available memory, in bytes */
    private Pair<Long, Long> memoryStatus;

    /** Total, Available swap, in bytes */
    private Pair<Long, Long> swapStatus;

    /** Total, Available space per-mount point */
    private Map<String, Pair<Long, Long>> diskStatus;
}
