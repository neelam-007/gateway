/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.util.Pair;

import java.util.*;

/**
 * A registry of well-known {@link Monitorable monitorables}.  Try to use these instances rather than cooking up your own. 
 */
public final class BuiltinMonitorables {
    public static final MonitorableProperty CPU_IDLE = new MonitorableProperty(ComponentType.HOST, "cpuIdle", Integer.class);
    public static final MonitorableProperty CPU_TEMPERATURE = new MonitorableProperty(ComponentType.HOST, "cpuTemperature", Integer.class);
    public static final MonitorableProperty SWAP_SPACE = new MonitorableProperty(ComponentType.HOST, "swapSpace", Long.class);
    public static final MonitorableProperty DISK_FREE = new MonitorableProperty(ComponentType.HOST, "diskFree", Long.class);
    public static final MonitorableProperty TIME = new MonitorableProperty(ComponentType.HOST, "time", Long.class);
    public static final MonitorableProperty LOG_SIZE = new MonitorableProperty(ComponentType.HOST, "logFileSize", Long.class);
    public static final MonitorableProperty NODE_STATE = new MonitorableProperty(ComponentType.NODE, "nodeState", NodeStateType.class);
    public static final MonitorableProperty NTP_STATUS = new MonitorableProperty(ComponentType.HOST, "ntpStatus", NtpStatus.class);
    public static final MonitorableProperty RAID_STATUS = new MonitorableProperty(ComponentType.HOST, "raidStatus", RaidStatus.class);

    private static final Monitorable[] VALUES = new Monitorable[] {
        CPU_IDLE, CPU_TEMPERATURE, SWAP_SPACE, DISK_FREE, TIME, LOG_SIZE, NODE_STATE, NTP_STATUS, RAID_STATUS
    };

    private final Map<Pair<ComponentType, String>, MonitorableProperty> builtinProperties;
    private final Map<Pair<ComponentType, String>, MonitorableEvent> builtinEvents;

    public static MonitorableProperty getBuiltinProperty(ComponentType type, String name) {
        return InstanceHolder.INSTANCE.builtinProperties.get(new Pair<ComponentType, String>(type, name));
    }

    public static MonitorableEvent getBuiltinEvents(ComponentType type, String name) {
        return InstanceHolder.INSTANCE.builtinEvents.get(new Pair<ComponentType, String>(type, name));
    }

    private BuiltinMonitorables() {
        final Map<Pair<ComponentType, String>, MonitorableProperty> props = new HashMap<Pair<ComponentType, String>, MonitorableProperty>();
        final Map<Pair<ComponentType, String>, MonitorableEvent> events = new HashMap<Pair<ComponentType, String>, MonitorableEvent>();

        for (Monitorable value : VALUES) {
            if (value instanceof MonitorableProperty) {
                MonitorableProperty property = (MonitorableProperty) value;
                props.put(new Pair<ComponentType, String>(property.getComponentType(), property.getName()), property);
            } else if (value instanceof MonitorableEvent) {
                MonitorableEvent event = (MonitorableEvent) value;
                events.put(new Pair<ComponentType, String>(event.getComponentType(), event.getName()), event);
            }
        }

        this.builtinProperties = Collections.unmodifiableMap(props);
        this.builtinEvents = Collections.unmodifiableMap(events);
    }

    private static class InstanceHolder {
        private static final BuiltinMonitorables INSTANCE = new BuiltinMonitorables();
    }

}
