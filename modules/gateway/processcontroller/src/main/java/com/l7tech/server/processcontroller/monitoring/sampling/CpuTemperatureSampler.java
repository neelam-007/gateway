/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import java.util.regex.Pattern;

/**
 *
 */
class CpuTemperatureSampler extends HostPropertySampler<Long> {
    private static final String PROC_TEMP = "/proc/acpi/thermal_zone/THRM/temperature";
    private static final Pattern TEMP_MATCHER = Pattern.compile("^temperature:\\s*(\\d+)\\s*C\\s*$");

    public CpuTemperatureSampler(String componentId) {
        super(componentId, "cpuTemperature");
    }

    public Long sample() throws PropertySamplingException {
        return matchNumberFromFile(PROC_TEMP, TEMP_MATCHER);
    }
}
