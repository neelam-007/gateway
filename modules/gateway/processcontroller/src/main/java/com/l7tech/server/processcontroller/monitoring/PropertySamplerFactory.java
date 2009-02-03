/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.api.monitoring.MonitorableProperty;

import java.io.Serializable;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.lang.reflect.Constructor;

public final class PropertySamplerFactory {
    private static final Map<String, Class<? extends HostPropertySampler>> hostPropertySamplers = Collections.unmodifiableMap(new HashMap<String, Class<? extends HostPropertySampler>>() {{
        put("cpuIdle", CpuIdleSampler.class);
        put("swapSpace", SwapSpaceSampler.class);
        put("cpuTemperature", CpuTemperatureSampler.class);
        put("diskFree", DiskFreeSampler.class);
        put("time", TimeSampler.class);
    }});

    public <V extends Serializable> PropertySampler<V> makeSampler(MonitorableProperty property, String componentId) {
        final String propertyName = property.getName();
        switch(property.getComponentType()) {
            case HOST:
                final Class<? extends HostPropertySampler> samplerClass = hostPropertySamplers.get(propertyName);
                if (samplerClass == null) throw new IllegalArgumentException("Unsupported host property: " + propertyName);
                try {
                    Constructor<? extends HostPropertySampler> ctor = samplerClass.getConstructor(String.class);
                    return ctor.newInstance(componentId); // TODO caching here?
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to create sampler for host property " + propertyName, e);
                }
            default:
                // TODO
                throw new UnsupportedOperationException("Only host properties are currently supported");
        }
    }
}
