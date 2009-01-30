package com.l7tech.server.processcontroller.monitoring;

import java.io.IOException;

/**
 * Sampler of the PC node's current system time, so the ESM can display relative clock skew.
 */
public class TimeSampler extends NodePropertySampler {
    protected TimeSampler(String componentId) {
        super(componentId, "time");
    }

    Long sample() throws PropertySamplingException {
        return System.currentTimeMillis();
    }

    public void close() throws IOException {
    }
}
