package com.l7tech.server.ems.enterprise;

/**
 * JSON string constants to match those defined in JavaScripts.
 *
 * @since Enterprise Manager 1.0
 * @author rmak
 */
public final class JSONConstants {
    /**
     * For conversion of a Java Throwable to JSON notation.
     */
    public final class Exception {
        public static final String EXCEPTION         = "exception";
        public static final String MESSAGE           = "message";
        public static final String LOCALIZED_MESSAGE = "localizedMessage";
        public static final String STACK_TRACE       = "stackTrace";
        public static final String CAUSE             = "cause";
    }

    /**
     * Monitoring property names for SSG Cluster entities.
     * Defined as l7.EntityTreeTable.SSG_CLUSTER_MONITORING_PROPERTY in entityTreeTable.js.
     */
    public final class SsgClusterMonitoringProperty {
        public static final String AUDIT_SIZE = "auditSize";
    }

    /**
     * Monitoring property names for SSG Node entities.
     * Defined as l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY in entityTreeTable.js.
     */
    public final class SsgNodeMonitoringProperty {
        public static final String LOG_SIZE    = "logSize";
        public static final String DISK_USED   = "diskUsed";
        public static final String DISK_FREE   = "diskFree";
        public static final String RAID_STATUS = "raidStatus";
        public static final String CPU_TEMP    = "cpuTemp";
        public static final String CPU_USAGE   = "cpuUsage";
        public static final String CLOCK_DRIFT = "clockDrift";
    }

    /**
     * Entity types.
     * Defined as l7.EntityTreeTable.ENTITY in entityTreeTable.js.
     */
    public final class Entity {
        public static final String ENTERPRISE_FOLDER = "enterpriseFolder";
        public static final String SSG_CLUSTER       = "ssgCluster";
        public static final String SSG_NODE          = "ssgNode";
        public static final String SSG_FOLDER        = "ssgFolder";
        public static final String PUBLISHED_SERVICE = "publishedService";
        public static final String POLICY_FRAGMENT   = "policyFragment";
    }

    // Properties for all entities.
    public static final String ID        = "id";
    public static final String PARENT_ID = "parentId";
    public static final String TYPE      = "type";
    public static final String NAME      = "name";

    // Additional properties for SSG Cluster entities.
    public static final String ANCESTORS     = "ancestors";
    public static final String SSL_HOST_NAME = "sslHostName";
    public static final String ADMIN_PORT    = "adminPort";
    public static final String DB_HOSTS      = "dbHosts";

    // Additional properties for SSG Node entities.
    public static final String SELF_HOST_NAME = "selfHostName";

    // Additional properties for enterprise folder and SSG Cluster entities.
    public static final String ACCESS_STATUS = "accessStatus";

    // Additional properties for SSG Cluster and SSG Node entities.
    public static final String ONLINES_TATUS        = "onlineStatus";
    public static final String TRUST_STATUS         = "trustStatus";
    public static final String IP_ADDRESS           = "ipAddress";
    public static final String MONITORED_PROPERTIES = "monitoredProperties";

    // Additional properties for enterprise folders, SSG Clusters, service folders, published services or policy fragments.
    public static final String RBAC_CUD = "rbacCUD";

    // Properties within a monitored property.
    public static final String MONITORED = "monitored";
    public static final String VALUE     = "value";
    public static final String CRITICAL  = "critical";

    // Additional properties for SSG Cluster, SSG Node, published service and policy fragment entities.
    public static final String VERSION = "version";
}
