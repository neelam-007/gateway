/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.server.management.NodeStateType;

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

    private BuiltinMonitorables() { }
}
