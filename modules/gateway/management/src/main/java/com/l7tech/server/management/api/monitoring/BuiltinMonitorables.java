package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.util.ComparisonOperator;
import com.l7tech.util.Pair;

import java.util.*;

/**
 * A registry of well-known {@link Monitorable monitorables}.  Try to use these instances rather than cooking up your own. 
 */
public final class BuiltinMonitorables {
    /** Number of records currently in the cluster's audit table */
    public static final MonitorableProperty AUDIT_SIZE = new MonitorableProperty(ComponentType.CLUSTER, "auditSize", Long.class, "records", 30000L, ComparisonOperator.GE, "100000");
    /** Number of records currently in the cluster's audit table */
    public static final MonitorableProperty DATABASE_REPLICATION_DELAY = new MonitorableProperty(ComponentType.CLUSTER, "databaseReplicationDelay", Long.class, "seconds", 30000L, ComparisonOperator.GE, "100000");
    /** Current state of Gateway node */
    public static final MonitorableProperty NODE_STATE = new MonitorableProperty(ComponentType.NODE, "operatingStatus", NodeStateType.class, null, 10000L, ComparisonOperator.NE, NodeStateType.RUNNING.name());
    /** Size of log files, in KiB */
    public static final MonitorableProperty LOG_SIZE = new MonitorableProperty(ComponentType.HOST, "logSize", Long.class, "KB", 20000L, ComparisonOperator.GT, "500000");
    /** Percent free space */
    public static final MonitorableProperty DISK_USAGE_PERCENT = new MonitorableProperty(ComponentType.HOST, "diskUsage", Integer.class, "%", 30000L, ComparisonOperator.GT, "80");
    /** Free disk space, in KiB */
    public static final MonitorableProperty DISK_FREE_KIB = new MonitorableProperty(ComponentType.HOST, "diskFree", Long.class, "KB", 30000L, ComparisonOperator.LT, "1000000");
    /** Current status of appliance's RAID array */
    public static final MonitorableProperty RAID_STATUS = new MonitorableProperty(ComponentType.HOST, "raidStatus", RaidStatus.class, null, 10000L, ComparisonOperator.EQ, RaidStatus.BAD.name());
    /** CPU Temperature, in °C */
    public static final MonitorableProperty CPU_TEMPERATURE = new MonitorableProperty(ComponentType.HOST, "cpuTemp", Integer.class, "degrees Celsius", 10000L, ComparisonOperator.GE, "55");
    /** CPU usage percentage */
    public static final MonitorableProperty CPU_USAGE = new MonitorableProperty(ComponentType.HOST, "cpuUsage", Integer.class, "%", 15000L, ComparisonOperator.GE, "98");
    /** Free Swap space, in KiB */
    public static final MonitorableProperty SWAP_USAGE_KIB = new MonitorableProperty(ComponentType.HOST, "swapUsage", Long.class, "KB", 60000L, ComparisonOperator.GT, "250000");
    /** Current NTP synchronization status of SSG appliance */
    public static final MonitorableProperty NTP_STATUS = new MonitorableProperty(ComponentType.HOST, "ntpStatus", NtpStatus.class, null, 15000L, ComparisonOperator.NE, NtpStatus.OK.name());
    /** Current time, in milliseconds GMT since Unix epoch */
    public static final MonitorableProperty TIME = new MonitorableProperty(ComponentType.HOST, "time", Long.class, "ms since epoch", 5000L, ComparisonOperator.LT, "1");

    private static final Monitorable[] VALUES = new Monitorable[] {
            CPU_USAGE, CPU_TEMPERATURE, SWAP_USAGE_KIB, DISK_FREE_KIB, DISK_USAGE_PERCENT, TIME, LOG_SIZE, NODE_STATE, NTP_STATUS, RAID_STATUS, AUDIT_SIZE, DATABASE_REPLICATION_DELAY
    };

    private final Map<Pair<ComponentType, String>, MonitorableProperty> builtinProperties;
    private final Map<String, Set<MonitorableProperty>> builtinPropertiesByName;
    private final Map<Pair<ComponentType, String>, MonitorableEvent> builtinEvents;

    public static MonitorableProperty getBuiltinProperty(ComponentType type, String name) {
        return InstanceHolder.INSTANCE.builtinProperties.get(new Pair<ComponentType, String>(type, name));
    }

    public static Set<MonitorableProperty> getBuiltinPropertiesByName(String name) {
        Set<MonitorableProperty> ret = InstanceHolder.INSTANCE.builtinPropertiesByName.get(name);
        return ret == null ? Collections.<MonitorableProperty>emptySet() : ret;
    }

    public static MonitorableProperty getAtMostOneBuiltinPropertyByName(String name) throws IllegalArgumentException {
        Set<MonitorableProperty> ret = getBuiltinPropertiesByName(name);
        if (ret.isEmpty())
            return null;
        if (ret.size() == 1)
            return ret.iterator().next();
        throw new IllegalArgumentException("More than one well-known property with name " + name);
    }

    public static MonitorableEvent getBuiltinEvents(ComponentType type, String name) {
        return InstanceHolder.INSTANCE.builtinEvents.get(new Pair<ComponentType, String>(type, name));
    }

    private BuiltinMonitorables() {
        final Map<Pair<ComponentType, String>, MonitorableProperty> props = new HashMap<Pair<ComponentType, String>, MonitorableProperty>();
        final Map<String, Set<MonitorableProperty>> propsByName = new HashMap<String, Set<MonitorableProperty>>();
        final Map<Pair<ComponentType, String>, MonitorableEvent> events = new HashMap<Pair<ComponentType, String>, MonitorableEvent>();

        for (Monitorable value : VALUES) {
            if (value instanceof MonitorableProperty) {
                MonitorableProperty property = (MonitorableProperty) value;
                final String name = property.getName();
                props.put(new Pair<ComponentType, String>(property.getComponentType(), name), property);
                Set<MonitorableProperty> set = propsByName.get(name);
                if (set == null) {
                    set = new HashSet<MonitorableProperty>();
                    propsByName.put(name, set);
                }
                set.add(property);
            } else if (value instanceof MonitorableEvent) {
                MonitorableEvent event = (MonitorableEvent) value;
                events.put(new Pair<ComponentType, String>(event.getComponentType(), event.getName()), event);
            }
        }

        this.builtinProperties = Collections.unmodifiableMap(props);
        this.builtinPropertiesByName = Collections.unmodifiableMap(propsByName);
        this.builtinEvents = Collections.unmodifiableMap(events);
    }

    private static class InstanceHolder {
        private static final BuiltinMonitorables INSTANCE = new BuiltinMonitorables();
    }

}
