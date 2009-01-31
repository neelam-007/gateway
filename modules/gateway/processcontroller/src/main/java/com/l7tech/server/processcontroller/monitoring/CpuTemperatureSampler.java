package com.l7tech.server.processcontroller.monitoring;

import java.util.regex.Pattern;

/**
 *
 */
public class CpuTemperatureSampler extends HostPropertySampler<Long> {
    private static final String PROC_TEMP = "/proc/acpi/thermal_zone/THRM/temperature";
    private static final Pattern TEMP_MATCHER = Pattern.compile("^temperature:\\s*(\\d+)\\s*C\\s*$");

    public CpuTemperatureSampler(String componentId) {
        super(componentId, "cpuTemperature");
    }

    Long sample() throws PropertySamplingException {
        return matchNumberFromFile(PROC_TEMP, TEMP_MATCHER);
    }
}
