package com.l7tech.server.processcontroller.monitoring;

/**
 * Sampler of the PC host's current system time, so the ESM can display relative clock skew.
 */
class TimeSampler extends HostPropertySampler<Long> {
    protected TimeSampler(String componentId) {
        super(componentId, "time");
    }

    Long sample() throws PropertySamplingException {
        return System.currentTimeMillis();
    }
}
