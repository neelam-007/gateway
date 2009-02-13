package com.l7tech.server.ems.enterprise;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    public static final class Exception {
        public static final String EXCEPTION         = "exception";
        public static final String MESSAGE           = "message";
        public static final String LOCALIZED_MESSAGE = "localizedMessage";
        public static final String STACK_TRACE       = "stackTrace";
        public static final String CAUSE             = "cause";
    }

    /**
     * Enum of system monitoring setup settings.
     */
    public static final class SystemMonitoringSetup {
        public static final String PROPERTY_SETUP                = "propertySetup";
        public static final String SAMPLING_INTERVAL_LOWER_LIMIT = "samplingIntervalLowerLimit";
        public static final String DISABLE_ALL_NOTIFICATIONS     = "disableAllNotifications";
        public static final String AUDIT_UPON_ALERT_STATE        = "auditUponAlertState";
        public static final String AUDIT_UPON_NORMAL_STATE       = "auditUponNormalState";
        public static final String AUDIT_UPON_NOTIFICATION       = "auditUponNotification";
    }

    /**
     * Enum of monitoring property types for SSG Cluster entities.
     * Defined as l7.Constants.SSG_CLUSTER_MONITORING_PROPERTY in l7.js.
     */
    public static final class SsgClusterMonitoringProperty {
        public static final String AUDIT_SIZE = "auditSize";
    }

    /**
     * Enum of monitoring property types for SSG Node entities.
     * Defined as l7.Constants.SSG_NODE_MONITORING_PROPERTY in l7.js.
     */
    public static final class SsgNodeMonitoringProperty {
        public static final String OPERATING_STATUS = "operatingStatus";
        public static final String LOG_SIZE         = "logSize";
        public static final String DISK_USAGE       = "diskUsage";
        public static final String DISK_FREE        = "diskFree";
        public static final String RAID_STATUS      = "raidStatus";
        public static final String CPU_TEMP         = "cpuTemp";
        public static final String CPU_USAGE        = "cpuUsage";
        public static final String SWAP_USAGE       = "swapUsage";
        public static final String NTP_STATUS       = "ntpStatus";
    }

    /**
     * Enum of settings of each propertySetup.
     */
    public static final class MonitoringPropertySettings {
        public static final String SAMPLING_INTERVAL         = "samplingInterval";
        public static final String DEFAULT_TRIGGER_VALUE     = "defaultTriggerValue";
        public static final String TRIGGER_VALUE_LOWER_LIMIT = "triggerValueLowerLimit";
        public static final String TRIGGER_VALUE_UPPER_LIMIT = "triggerValueUpperLimit";
        public static final String UNIT                      = "unit";
    }

    /**
     * Enum of notifiation types.
     */
    public static final class NotifiationRule {
        public static final String RECORDS = "records";
        public static final String PARAMS  = "params";
    }

    /**
     * Enum of notifiation types.
     * Defined as l7.Constants.NOTIFICATION_TYPE in l7.js.
     */
    public static final class NotificationType {
        public static final String E_MAIL       = "eMail";
        public static final String SNMP_TRAP    = "snmpTrap";
        public static final String HTTP_REQUEST = "httpRequest";
    }

    /**
     * Enum of parameters for e-mail notification.
     */
    public static final class NotificationEmailParams {
        public static final String PROTOCOL      = "protocol";
        public static final String HOST          = "host";
        public static final String PORT          = "port";
        public static final String REQUIRES_AUTH = "requiresAuthentication";
        public static final String USERNAME      ="userName";
        public static final String PASSWORD      = "password";
        public static final String FROM          = "from";
        public static final String TO            = "to";
        public static final String CC            = "cc";
        public static final String BCC           = "bcc";
        public static final String SUBJECT       = "subject";
        public static final String BODY          = "body";
    }

    /**
     * Enum of protocol types for e-mail notification.
     * Defined as l7.Constants.NOTIFICATION_EMAIL_PROTOCOL in l7.js.
     */
    public static final class NotificationEmailProtocol {
        public static final String PLAIN_SMTP         = "plainSmtp";
        public static final String SMTP_OVER_SSL      = "sslSmtp";
        public static final String SMTP_WITH_STARTTLS = "startTlsSmtp";
    }

    /**
     * Enum of parameters for snmp-trap notification.
     */
    public static final class NotificationSnmpTrapParams {
        public static final String HOST         = "host";
        public static final String DEFAULT_PORT = "defaultPort";
        public static final String PORT         = "port";
        public static final String COMMUNITY    = "community";
        public static final String TEXTDATA     = "textData";
        public static final String OIDFUFFIX    = "oidSuffix";
    }

    /**
     * Enum of parameters for http-request notification.
     */
    public static final class NotificationHttpRequestParams {
        public static final String URL                   = "url";
        public static final String HTTP_METHOD           = "httpMethod";
        public static final String STANDARD_CONTENT_TYPE = "standardContentType";
        public static final String CONTENT_TYPE          = "contentType";
        public static final String BODAY                 = "body";
    }

    /**
     * Enum of notification http method types.
     */
    public static final class NotificationHttpMethodType {
        public static final String GET    = "GET";
        public static final String POST   = "POST";
        public static final String PUT    = "PUT";
        public static final String DELETE = "DELETE";
        public static final String HEAD   = "HEAD";
        public static final String OTHER  = "OTHER";
    }

    /**
     * Enum of SSG Cluster online states.
     * Defined as l7.Constants.SSG_CLUSTER_ONLINE_STATE in l7.js.
     */
    public static final class SsgClusterOnlineState {
        public static final String UP      = "up";
        public static final String PARTIAL = "partial";
        public static final String DOWN    = "down";
    }

    /**
     * Enum of SSG Node online states.
     * Defined as l7.Constants.SSG_NODE_ONLINE_STATE in l7.js.
     */
    public static final class SsgNodeOnlineState {
        public static final String ON      = "on";
        public static final String OFF     = "off";
        public static final String DOWN    = "down";
        public static final String OFFLINE = "offline";
    }

    /**
     * Entity types.
     * Defined as l7.Constants.ENTITY_TYPE in l7.js.
     */
    public static final class EntityType {
        public static final String ENTERPRISE_FOLDER       = "enterpriseFolder";
        public static final String SSG_CLUSTER             = "ssgCluster";
        public static final String SSG_NODE                = "ssgNode";
        public static final String SSG_FOLDER              = "ssgFolder";
        public static final String PUBLISHED_SERVICE       = "publishedService";
        public static final String PUBLISHED_SERVICE_ALIAS = "publishedServiceAlias";
        public static final String OPERATION               = "operation";
        public static final String POLICY_FRAGMENT         = "policyFragment";
        public static final String POLICY_FRAGMENT_ALIAS   = "policyFragmentAlias";
        public static final String SERVICE_FOLDER          = "serviceFolder";

        public static final Map<String, com.l7tech.objectmodel.EntityType> ENTITY_TYPE_MAP;
        static {
            Map<String, com.l7tech.objectmodel.EntityType> entityTypeMap = new HashMap<String, com.l7tech.objectmodel.EntityType>();
            entityTypeMap.put( ENTERPRISE_FOLDER, com.l7tech.objectmodel.EntityType.ESM_ENTERPRISE_FOLDER );
            entityTypeMap.put( SSG_CLUSTER, com.l7tech.objectmodel.EntityType.ESM_SSG_CLUSTER );
            entityTypeMap.put( SSG_NODE, com.l7tech.objectmodel.EntityType.ESM_SSG_NODE );
            entityTypeMap.put( SSG_FOLDER, com.l7tech.objectmodel.EntityType.FOLDER );
            entityTypeMap.put( PUBLISHED_SERVICE, com.l7tech.objectmodel.EntityType.SERVICE );
            entityTypeMap.put( PUBLISHED_SERVICE_ALIAS, com.l7tech.objectmodel.EntityType.SERVICE_ALIAS );
            entityTypeMap.put( POLICY_FRAGMENT, com.l7tech.objectmodel.EntityType.POLICY );
            entityTypeMap.put( POLICY_FRAGMENT_ALIAS, com.l7tech.objectmodel.EntityType.POLICY_ALIAS );
            entityTypeMap.put( SERVICE_FOLDER, com.l7tech.objectmodel.EntityType.FOLDER );
            ENTITY_TYPE_MAP = Collections.unmodifiableMap(entityTypeMap);
        }
    }

    // Properties for all entities.
    public static final String ID         = "id";
    public static final String RELATED_ID = "relatedId";
    public static final String PARENT_ID  = "parentId";
    public static final String TYPE       = "type";
    public static final String NAME       = "name";
    public static final String READONLY   = "readOnly";
    public static final String ANCESTORS  = "ancestors";
    public static final String ENTITY     = "entity";

    // Additional properties for SSG Cluster entities.
    public static final String CLUSTER_NAME      = "clusterName";
    public static final String CLUSTER_ANCESTORS = "clusterAncestors";
    public static final String SSL_HOST_NAME     = "sslHostName";
    public static final String ADMIN_PORT        = "adminPort";
    public static final String ADMIN_APPLET_PORT = "adminAppletPort";
    public static final String DB_HOSTS          = "dbHosts";

    // Additional properties for SSG Node entities.
    public static final String SELF_HOST_NAME = "selfHostName";
    public static final String GATEWAY_PORT = "gatewayPort";
    public static final String PROCESS_CONTROLLER_PORT = "processControllerPort";

    // Additional properties for enterprise folder and SSG Cluster entities.
    public static final String ACCESS_STATUS = "accessStatus";

    // Additional properties for SSG Cluster and SSG Node entities.
    public static final String ONLINE_STATUS         = "onlineStatus";
    public static final String TRUST_STATUS          = "trustStatus";
    public static final String IP_ADDRESS            = "ipAddress";
    public static final String MONITORING_PROPERTIES = "monitoringProperties";

    // Additional properties for enterprise folders, SSG Clusters, service folders, published services or policy fragments.
    public static final String RBAC_CUD = "rbacCUD";

    // Properties within a monitored property current value.
    public static final String MONITORED = "monitored";
    public static final String VALUE     = "value";
    public static final String UNIT      = "unit";
    public static final String ALERT     = "alert";

    // Additional properties for SSG Cluster, SSG Node, published service and policy fragment entities.
    public static final String VERSION = "version";

    /**
     * Enum of Entity Prop Setup.
     */
    public static final class ENTITY_PROPS_SETUP {
        public static final String ENTITY               = "entity";
        public static final String PROP_TYPE            = "propertyType";
        public static final String MONITORING_ENABLED   = "monitoringEnabled";
        public static final String TRIGGER_ENABLED      = "triggerEnabled";
        public static final String TRIGGER_VALUE        = "triggerValue";
        public static final String UNIT                 = "unit";
        public static final String NOTIFICATION_ENABLED = "notificationEnabled";
        public static final String NOTIFICATION_RULES   = "notificationRules";
    }

    /**
     * Enum of standard report types.
     * Defined as l7.Constants.REPORT_TYPE in l7.js.
     */
    public static final class ReportType {
        public static final String PERFORMANCE = "performance";
        public static final String USAGE       = "usage";
    }

    /**
     * Enum of time period types in standard reports.
     * Defined as l7.Constants.REPORT_TIME_PERIOD_TYPE in l7.js.
     */
    public static final class TimePeriodTypeValues {
        public static final String RELATIVE = "relative";
        public static final String ABSOLUTE = "absolute";
    }

    public static final class TimePeriodTypeKeys{
        public static final String TIME_PERIOD_MAIN = "timePeriod";
        public static final String TYPE = "type";
        public static final String TIME_INTERVAL = "timeInterval";
    }

    public static final class TimePeriodRelativeKeys{
        public static final String UNIT_OF_TIME = "unitOfTime";
        public static final String NUMBER_OF_TIME_UNITS = "numberOfTimeUnits";
        public static final String TIME_ZONE = "timeZone";
        public static final String [] ALL_KEYS = new String[]{UNIT_OF_TIME, NUMBER_OF_TIME_UNITS, TIME_ZONE};
    }

    public static final class TimePeriodIntervalKeys{
        public static final String INTERVAL_UNIT_OF_TIME = "unit";
        public static final String NUMBER_OF_INTERVAL_TIME_UNITS = "value";
        public static final String [] ALL_KEYS = new String[]{INTERVAL_UNIT_OF_TIME, NUMBER_OF_INTERVAL_TIME_UNITS};
    }

    public static final class TimePeriodAbsoluteKeys{
        public static final String START = "start";
        public static final String END = "end";
        public static final String TIME_ZONE = "timeZone";
        public static final String [] ALL_KEYS = new String[]{START, END, TIME_ZONE};
    }

    public static final class ReportEntities{
        public static final String CLUSTER_ID = "clusterId";
        public static final String PUBLISHED_SERVICE_ID = "publishedServiceId";
        public static final String PUBLISHED_SERVICE_NAME = "publishedServiceName";
        public static final String RELATED_ID = "relatedId";
        public static final String OPERATION = "operation";
        public static final String [] ALL_KEYS = new String[]{ CLUSTER_ID, PUBLISHED_SERVICE_ID, PUBLISHED_SERVICE_NAME, RELATED_ID, OPERATION};
    }

    public static final class ReportMappings{
        public static final String CLUSTER_ID = "clusterId";
        public static final String MESSAGE_CONTEXT_KEY = "messageContextKey";
        public static final String CONSTRAINT = "constraint";
        public static final String [] ALL_KEYS = new String[]{ CLUSTER_ID, MESSAGE_CONTEXT_KEY, CONSTRAINT};
    }

    public final static String REPORT_TYPE = "reportType";
    public final static String SUMMARY_REPORT = "summaryReport";
    public final static String REPORT_RAN_BY = "reportRanBy";

    public final static String REPORT_ENTITIES = "entities";
    public static final String GROUPINGS = "groupings";

    public static final String AUTH_USER = "AUTH_USER";

    public static final String SUMMARY_CHART = "summaryChart";

    public static final String ENTITY_TYPE = "entityType";

    public static final String REPORT_NAME = "reportName";

    public static final String REPORT_SETTINGS_WARNING_ITEMS = "warningItems";
}
