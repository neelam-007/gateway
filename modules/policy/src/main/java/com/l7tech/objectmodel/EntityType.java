/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import java.util.Comparator;

/**
 * Enum of all entity types known to the RBAC system.
 */
public enum EntityType implements Comparable<EntityType> {
    ANY("<any>", true),

    ID_PROVIDER_CONFIG("Identity Provider", true),
    USER("User", true),
    GROUP("Group", true),
    SERVICE("Published Service", true),
    SERVICE_ALIAS("Published Service Alias", true),
    JMS_CONNECTION("JMS Connection", true),
    JMS_ENDPOINT("JMS Endpoint", true),
    TRUSTED_CERT("Trusted Certificate", true),
    REVOCATION_CHECK_POLICY("Revocation Check Policy", true),
    SSG_KEY_ENTRY("Private Key", true),
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
    CLUSTER_INFO("Cluster Node Information", true),
    SERVICE_USAGE("Service Usage Record", true),
    SCHEMA_ENTRY("Schema Entry", true),
    METRICS_BIN("Service Metrics Bin", true),

    RBAC_ROLE("Access Control Role", true),

    AUDIT_MESSAGE("Audit Record (Message)", true),
    AUDIT_ADMIN("Audit Record (Admin)", true),
    AUDIT_SYSTEM("Audit Record (System)", true),
    AUDIT_RECORD("Audit Record <any type>", true),

    LOG_RECORD("Log Record", true),

    SSG_CONNECTOR("Listen Port", true),

    EMAIL_LISTENER("Email Listener", true),

    LOG_SINK("Log Sink", true),

    SERVICE_TEMPLATE("Service Template", true),

    TRUSTED_EMS("Trusted EMS", true),
    TRUSTED_EMS_USER("Trusted EMS User", true),

    ESM_SSG_CLUSTER("Cluster", false),
    ;

    private final String name;
    private final boolean displayedInGui;

    public String getName() {
        return name;
    }

    private EntityType(String name, boolean displayInGui) {
        this.name = name;
        this.displayedInGui = displayInGui;
    }

    public boolean isDisplayedInGui() {
        return displayedInGui;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final NameComparator NAME_COMPARATOR = new NameComparator();

    private static class NameComparator implements Comparator<EntityType> {
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
