/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PropertySamplerFactory implements ApplicationContextAware {
    private ApplicationContext spring;

    private static final Map<MonitorableProperty, Class<? extends PropertySampler>> hostPropertySamplers = Collections.unmodifiableMap(new HashMap<MonitorableProperty, Class<? extends PropertySampler>>() {{
        put(BuiltinMonitorables.AUDIT_SIZE, AuditSizeSampler.class);
        put(BuiltinMonitorables.CPU_TEMPERATURE, CpuTemperatureSampler.class);
        put(BuiltinMonitorables.CPU_USAGE, CpuUsageSampler.class);
        put(BuiltinMonitorables.DISK_FREE_KIB, DiskFreeSampler.class);
        put(BuiltinMonitorables.DISK_USAGE_PERCENT, DiskUsagePercentSampler.class);
        put(BuiltinMonitorables.LOG_SIZE, LogFileSampler.class);
        put(BuiltinMonitorables.NODE_STATE, NodeStateSampler.class);
        put(BuiltinMonitorables.NTP_STATUS, NtpStatusSampler.class);
        put(BuiltinMonitorables.RAID_STATUS, RaidStatusSampler.class);
        put(BuiltinMonitorables.SWAP_USAGE_KIB, SwapUsageSampler.class);
        put(BuiltinMonitorables.TIME, TimeSampler.class);
    }});

    @SuppressWarnings({"unchecked"})
    public <V extends Serializable> PropertySampler<V> makeSampler(MonitorableProperty property, String componentId) {
        final Class<? extends PropertySampler> samplerClass = hostPropertySamplers.get(property);
        if (samplerClass == null) throw new IllegalArgumentException("Unsupported host property: " + property.getName());
        try {
            for (Constructor constructor : samplerClass.getConstructors()) {
                if (Arrays.equals(constructor.getParameterTypes(), new Class[] { String.class, ApplicationContext.class })) {
                    return (PropertySampler<V>) constructor.newInstance(componentId, spring);
                } else if (Arrays.equals(constructor.getParameterTypes(), new Class[] { String.class })) {
                    return (PropertySampler<V>) constructor.newInstance(componentId);
                }
            }
            throw new IllegalArgumentException("No matching constructor found in " + samplerClass);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create sampler for " + property.getComponentType() + " property " + property.getName(), e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.spring = applicationContext;
    }
}
