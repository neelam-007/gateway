/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

/**
 * Sampler of the PC host's current system time, so the ESM can display relative clock skew.
 */
class TimeSampler extends HostPropertySampler<Long> {
    protected TimeSampler(String componentId) {
        super(componentId, "time");
    }

    public Long sample() throws PropertySamplingException {
        return System.currentTimeMillis();
    }
}
