/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Comparator;

/**
 * Enum of all entity types known to the RBAC system.
 */
@XmlJavaTypeAdapter(EntityTypeAdapter.class)
public enum EntityType implements Comparable<EntityType> {
    ANY("<any>", "Objects", true),

    ID_PROVIDER_CONFIG("Identity Provider", true),
    USER("User", true), // TODO Don't change the string "User", it's in customer databases.
    GROUP("Group", true), // TODO Don't change the string "Group", it's in customer databases.
    SERVICE("Published Service", true),
    SERVICE_ALIAS("Published Service Alias", true),
    SERVICE_DOCUMENT("Service Document", true),
    SERVICE_OPERATION("Service Operation", true),
    JMS_CONNECTION("JMS Connection", true),
    JMS_ENDPOINT("JMS Endpoint", true),
    TRUSTED_CERT("Trusted Certificate", true),
    REVOCATION_CHECK_POLICY("Revocation Check Policy", true),
    SSG_KEY_ENTRY("Private Key", "Private Keys", true),
    ALERT_TRIGGER("Alert Event", false),
    ALERT_ACTION("Alert Notification", false),
    SAMPLE_MESSAGE("Sample Message", true),

    POLICY("Policy", true),
    POLICY_ALIAS("Policy Alias", true),
    POLICY_VERSION("Policy Version", true),
    FOLDER("Folder", true),

    MAP_ATTRIBUTE("Attribute Configuration", false),
    MAP_IDENTITY("Identity Provider Attribute Mapping", false),
    MAP_TOKEN("Security Token Attribute Mapping", false),

    CLUSTER_PROPERTY("Cluster Property", true),
    CLUSTER_INFO("Cluster Node Information", "Cluster Node Info Records", true),
    SERVICE_USAGE("Service Usage Record", true),
    SCHEMA_ENTRY("Schema Entry", true),
    METRICS_BIN("Service Metrics Bin", true),

    RBAC_ROLE("Access Control Role", true),

    AUDIT_MESSAGE("Audit Record (Message)", "Message Audit Records", true),
    AUDIT_ADMIN("Audit Record (Admin)", "Admin Audit Records", true),
    AUDIT_SYSTEM("Audit Record (System)", "System Audit Records", true),
    AUDIT_RECORD("Audit Record <any type>", "Audit Records", true),

    LOG_RECORD("Log Record", true),

    SSG_CONNECTOR("Listen Port", true),

    EMAIL_LISTENER("Email Listener", true),

    LOG_SINK("Log Sink", true),

    SERVICE_TEMPLATE("Service Template", true),

    TRUSTED_ESM("Trusted ESM", true),
    TRUSTED_ESM_USER("Trusted ESM User", true),

    ESM_ENTERPRISE_FOLDER("Folder", false),
    ESM_SSG_CLUSTER("Cluster", false),
    ESM_SSG_NODE("Node", false),
    ESM_STANDARD_REPORT("Report", false),
    ESM_MIGRATION_RECORD("Migration Record", false),
    ;

    private final String name;
    private final boolean displayedInGui;
    private final String pluralName;

    public String getName() {
        return name;
    }

    public String getPluralName() {
        return pluralName;
    }

    private EntityType(String name, boolean displayInGui) {
        this.name = name;
        if (name.endsWith("y"))
            this.pluralName = name.substring(0, name.length()-1) + "ies";
        else if (name.toLowerCase().endsWith("s")) {
            this.pluralName = name + "es";
        } else {
            this.pluralName = name + "s";
        }
        this.displayedInGui = displayInGui;
    }

    private EntityType(String name, String pluralName, boolean displayedInGui) {
        this.name = name;
        this.pluralName = pluralName;
        this.displayedInGui = displayedInGui;
    }

    public boolean isDisplayedInGui() {
        return displayedInGui;
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
