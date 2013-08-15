package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.HasFolder;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Comparator;

/**
 * Enum of all entity types known to the RBAC system.
 */
@XmlJavaTypeAdapter(EntityTypeAdapter.class)
public enum EntityType implements Comparable<EntityType> {
    ANY("<any>", "Objects", true, false),

    ID_PROVIDER_CONFIG("Identity Provider", true, false),
    USER("User", true, true), // TODO Don't change the string "User", it's in customer databases.
    GROUP("Group", true, true), // TODO Don't change the string "Group", it's in customer databases.
    SERVICE("Published Service", true, true),
    SERVICE_ALIAS("Published Service Alias", true, true),
    SERVICE_DOCUMENT("Service Document", false, false),
    JMS_CONNECTION("JMS Connection", true, true),
    JMS_ENDPOINT("JMS Endpoint", true, true),
    TRUSTED_CERT("Trusted Certificate", true, true),
    REVOCATION_CHECK_POLICY("Revocation Check Policy", true, true),
    SSG_KEY_ENTRY("Private Key", "Private Keys", true, true),
    SSG_KEY_METADATA("Private Key Metadata", false, false),
    SSG_KEYSTORE("Private Key Store", "Private Key Stores", true, true),
    ALERT_TRIGGER("Alert Event", false, false),
    ALERT_ACTION("Alert Notification", false, false),
    SAMPLE_MESSAGE("Sample Message", true, true),

    POLICY("Policy", true, true),
    POLICY_ALIAS("Policy Alias", true, true),
    POLICY_VERSION("Policy Version", false, true),
    FOLDER("Folder", true, true),
    ENCAPSULATED_ASSERTION("Encapsulated Assertion", true, true),

    MAP_ATTRIBUTE("Attribute Configuration", false, true),
    MAP_IDENTITY("Identity Provider Attribute Mapping", false, true),
    MAP_TOKEN("Security Token Attribute Mapping", false, true),

    CLUSTER_PROPERTY("Cluster Property", true, true),
    CLUSTER_INFO("Cluster Node Information", "Cluster Node Info Records", true, false),
    SERVICE_USAGE("Service Usage Record", true, false),
    METRICS_BIN("Service Metrics Bin", true, false),

    RBAC_ROLE("Access Control Role", true, true),

    AUDIT_MESSAGE("Audit Record (Message)", "Message Audit Records", true, false),
    AUDIT_ADMIN("Audit Record (Admin)", "Admin Audit Records", true, false),
    AUDIT_SYSTEM("Audit Record (System)", "System Audit Records", true, false),
    AUDIT_RECORD("Audit Record <any type>", "Audit Records", true, false),

    SSG_CONNECTOR("Listen Port", true, true),

    UDDI_REGISTRY("UDDI Registry", true, true),

    UDDI_PROXIED_SERVICE_INFO("UDDI Proxied Service Info", true, true),

    UDDI_SERVICE_CONTROL("UDDI Service Control", true, true),

    JDBC_CONNECTION("JDBC Connection", true, true),

    SITEMINDER_CONFIGURATION("SiteMinder Configuration", true, true),

    EMAIL_LISTENER("Email Listener", true, true),

    SSG_ACTIVE_CONNECTOR("Polling Listener", true, true),

    LOG_SINK("Log Sink", true, true),

    SERVICE_TEMPLATE("Service Template", true, true),

    TRUSTED_ESM("Trusted ESM", true, true),
    TRUSTED_ESM_USER("Trusted ESM User", true, true),

    ESM_ENTERPRISE_FOLDER("Folder", false, true),
    ESM_SSG_CLUSTER("Cluster", false, true),
    ESM_SSG_NODE("Node", false, true),
    ESM_STANDARD_REPORT("Report", false, true),
    ESM_MIGRATION_RECORD("Migration Record", false, true),
    ESM_NOTIFICATION_RULE("Notification Rule", false, true),
    ESM_LOG("Log Record", false, true), // used for ESM log RBAC
    VALUE_REFERENCE("Value Reference", false, false), // todo: better way to deal with mappable properties

    SECURE_PASSWORD("Secure Password", true, true),

    HTTP_CONFIGURATION("HTTP Options", "HTTP Options", true, true),
    RESOURCE_ENTRY("Global Resource", true, true),
    RESOLUTION_CONFIGURATION("Service Resolution Configuration", true, true),
    PASSWORD_POLICY("Password Policy", true, true),

    GENERIC("Generic Entity", "Generic Entities", true, true),
    FIREWALL_RULE("Firewall Rules Entity", "Firewall Rules Entities", true, false),
    ASSERTION_ACCESS("Assertion", "Assertions", true, true),
    SECURITY_ZONE("Security Zone", "Security Zones", true, true),
    CUSTOM_KEY_VALUE_STORE("Custom Key Value Store", "Custom Key Value Stores", true, true),
    LICENSE_DOCUMENT("License Document", false, false),
    ;

    private final String name;
    private final boolean displayedInGui;
    private final String pluralName;
    private final boolean allowSpecificScope;

    public String getName() {
        return name;
    }

    public String getPluralName() {
        return pluralName;
    }

    public boolean isAllowSpecificScope() {
        return allowSpecificScope;
    }

    private EntityType(String name, boolean displayInGui, boolean allowSpecificScope) {
        this.name = name;
        if (name.endsWith("y"))
            this.pluralName = name.substring(0, name.length()-1) + "ies";
        else if (name.toLowerCase().endsWith("s")) {
            this.pluralName = name + "es";
        } else {
            this.pluralName = name + "s";
        }
        this.displayedInGui = displayInGui;
        this.allowSpecificScope = allowSpecificScope;
    }

    private EntityType(String name, String pluralName, boolean displayedInGui, boolean allowSpecificScope) {
        this.name = name;
        this.pluralName = pluralName;
        this.displayedInGui = displayedInGui;
        this.allowSpecificScope = allowSpecificScope;
    }

    public boolean isDisplayedInGui() {
        return displayedInGui;
    }

    /**
     * @return true if entities of this type can be placed into security zones.
     */
    public boolean isSecurityZoneable() {
        final Class<? extends Entity> ec = getEntityClass();
        return (ec != null && ZoneableEntity.class.isAssignableFrom(ec));
    }

    /**
     * @return true if entities of this type can be placed into folders.
     */
    public boolean isFolderable() {
        final Class<? extends Entity> ec = getEntityClass();
        return (ec != null && HasFolder.class.isAssignableFrom(ec));
    }

    public static final NameComparator NAME_COMPARATOR = new NameComparator();

    private static class NameComparator implements Comparator<EntityType> {
        @Override
        public int compare(EntityType o1, EntityType o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public Class<? extends Entity> getEntityClass() {
        return EntityTypeRegistry.getEntityClass(this);
    }

    /**
     * Retrieves the EntityType associated with class that implements (or interface that extends) the Entity interface.
     *
     * @param entityClass   Entity whoose type is sought. Must not be null.
     * @return              The EntityType for the entityClass parameter.
     *
     * @see com.l7tech.objectmodel.EntityTypeRegistry
     */
    public static EntityType findTypeByEntity(Class<? extends Entity> entityClass) {
        EntityType type = EntityType.ANY;
        for ( EntityType et : EntityType.values() ) {
            if ( et == EntityType.ANY || et.getEntityClass() == null ) continue;
            if ( et.getEntityClass().isAssignableFrom( entityClass )) {
                type = et;
                break;
            }
        }
        return type;
    }
}
