package com.l7tech.util;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.GoidRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to retrieve the prefixes that were generated for existing entities during an 8.0.0 upgrade. If
 * this gateway never went through an 8.0.0 upgrade no prefixes will have been generated and the get prefix methods will
 * return null.
 *
 * @author Victor Kazakov
 */
public class GoidUpgradeMapper {
    private static final Map<String, Long> tableNamePrefixMap = new HashMap<>();
    private static boolean hasPrefixes = false;

    /**
     * This will check if prefixes were available and loaded.
     *
     * @return true if prefixes were loaded.
     */
    public static boolean hasPrefixes() {
        return hasPrefixes;
    }

    /**
     * This will check if the given prefix matched the upgrade prefix for the given entity type.
     *
     * @return true if prefix for the given entity matches the one given, false if the prefixes have not been loaded or
     *         if the prefixes don't match.
     */
    public static boolean prefixMatches(EntityType entityType, long prefix) {
        if (!hasPrefixes)
            return false;
        Long prefixFound = getPrefix(entityType);
        return prefixFound != null && prefixFound == prefix;
    }

    /**
     * Returns a prefix for a table given the table name. If the gateway never went through an 8.0.0 upgrade this will
     * always return null.
     *
     * @param tableName The table name to return the prefix for.
     * @return The prefix for the table name. Null if there is not prefix for the table name.
     */
    public static Long getPrefix(String tableName) {
        return (hasPrefixes && tableName != null) ? tableNamePrefixMap.get(tableName) : null;
    }

    /**
     * Returns a prefix for a table given the entity type. If the gateway never went through an 8.0.0 upgrade this will
     * always return null.
     *
     * @param entityType The entity type to get the prefix for.  If null, this method will return null.
     * @return The prefix for the entity type. Null if there is not a prefix for the given entity type.
     */
    public static Long getPrefix(EntityType entityType) {
        if (!hasPrefixes)
            return null;
        //Do not use a switch statement here, it will cause problems with the Obfuscator
        if (entityType == EntityType.JDBC_CONNECTION) {
            return tableNamePrefixMap.get("jdbc_connection");
        } else if (entityType == EntityType.SAMPLE_MESSAGE) {
            return tableNamePrefixMap.get("sample_messages");
        } else if (entityType == EntityType.CLUSTER_PROPERTY) {
            return tableNamePrefixMap.get("cluster_properties");
        } else if (entityType == EntityType.EMAIL_LISTENER) {
            return tableNamePrefixMap.get("email_listener");
        } else if (entityType == EntityType.GENERIC) {
            return tableNamePrefixMap.get("generic_entity");
        } else if (entityType == EntityType.SSG_CONNECTOR) {
            return tableNamePrefixMap.get("connector");
        } else if (entityType == EntityType.FIREWALL_RULE) {
            return tableNamePrefixMap.get("firewall_rule");
        } else if (entityType == EntityType.ENCAPSULATED_ASSERTION) {
            return tableNamePrefixMap.get("encapsulated_assertion");
        } else if (entityType == EntityType.JMS_CONNECTION) {
            return tableNamePrefixMap.get("jms_connection");
        } else if (entityType == EntityType.JMS_ENDPOINT) {
            return tableNamePrefixMap.get("jms_endpoint");
        } else if (entityType == EntityType.HTTP_CONFIGURATION) {
            return tableNamePrefixMap.get("http_configuration");
        } else if (entityType == EntityType.SSG_ACTIVE_CONNECTOR) {
            return tableNamePrefixMap.get("active_connector");
        } else if (entityType == EntityType.FOLDER) {
            return tableNamePrefixMap.get("folder");
        } else if (entityType == EntityType.POLICY) {
            return tableNamePrefixMap.get("policy");
        } else if (entityType == EntityType.POLICY_ALIAS) {
            return tableNamePrefixMap.get("policy_alias");
        } else if (entityType == EntityType.POLICY_VERSION) {
            return tableNamePrefixMap.get("policy_version");
        } else if (entityType == EntityType.SERVICE) {
            return tableNamePrefixMap.get("published_service");
        } else if (entityType == EntityType.SERVICE_DOCUMENT) {
            return tableNamePrefixMap.get("service_documents");
        } else if (entityType == EntityType.SERVICE_ALIAS) {
            return tableNamePrefixMap.get("published_service_alias");
        } else if (entityType == EntityType.SERVICE_USAGE) {
            return tableNamePrefixMap.get("service_usage");
        } else if (entityType == EntityType.UDDI_PROXIED_SERVICE_INFO) {
            return tableNamePrefixMap.get("uddi_proxied_service_info");
        } else if (entityType == EntityType.UDDI_REGISTRY) {
            return tableNamePrefixMap.get("uddi_registries");
        } else if (entityType == EntityType.UDDI_SERVICE_CONTROL) {
            return tableNamePrefixMap.get("uddi_service_control");
        } else if (entityType == EntityType.ASSERTION_ACCESS) {
            return tableNamePrefixMap.get("assertion_access");
        } else if (entityType == EntityType.AUDIT_ADMIN) {
            return tableNamePrefixMap.get("audit_admin");
        } else if (entityType == EntityType.AUDIT_MESSAGE) {
            return tableNamePrefixMap.get("audit_message");
        } else if (entityType == EntityType.AUDIT_RECORD) {
            return tableNamePrefixMap.get("audit_main");
        } else if (entityType == EntityType.TRUSTED_CERT) {
            return tableNamePrefixMap.get("trusted_cert");
        } else if (entityType == EntityType.CLUSTER_INFO) {
            return tableNamePrefixMap.get("cluster_info");
        } else if (entityType == EntityType.CUSTOM_KEY_VALUE_STORE) {
            return tableNamePrefixMap.get("custom_key_value_store");
        } else if (entityType == EntityType.ID_PROVIDER_CONFIG) {
            return tableNamePrefixMap.get("identity_provider");
        } else if (entityType == EntityType.SSG_KEYSTORE) {
            return tableNamePrefixMap.get("keystore_file");
        } else if (entityType == EntityType.SSG_KEY_METADATA) {
            return tableNamePrefixMap.get("keystore_key_metadata");
        } else if (entityType == EntityType.LICENSE_DOCUMENT) {
            return tableNamePrefixMap.get("license_document");
        } else if (entityType == EntityType.PASSWORD_POLICY) {
            return tableNamePrefixMap.get("password_policy");
        } else if (entityType == EntityType.RBAC_ROLE) {
            return tableNamePrefixMap.get("rbac_role");
        } else if (entityType == EntityType.RESOURCE_ENTRY) {
            return tableNamePrefixMap.get("resource_entry");
        } else if (entityType == EntityType.REVOCATION_CHECK_POLICY) {
            return tableNamePrefixMap.get("revocation_check_policy");
        } else if (entityType == EntityType.SECURE_PASSWORD) {
            return tableNamePrefixMap.get("secure_password");
        } else if (entityType == EntityType.SECURITY_ZONE) {
            return tableNamePrefixMap.get("security_zone");
        } else if (entityType == EntityType.SITEMINDER_CONFIGURATION) {
            return tableNamePrefixMap.get("siteminder_configuration");
        } else if (entityType == EntityType.TRUSTED_ESM) {
            return tableNamePrefixMap.get("trusted_esm");
        } else if (entityType == EntityType.TRUSTED_ESM_USER) {
            return tableNamePrefixMap.get("trusted_esm_user");
        } else {
            return null;
        }
    }

    /**
     * Map or wrap the specified legacy OID into a GOID, mapping it to the correct GOID for an entity from an updated
     * database, if applicable; otherwise, wrapping it as a Goid within the range reserved for a wrapped OID
     * (for temporary use within a Gateway upgraded from a pre-GOID database).
     * If a default oid is give (-1) the default goid will be returned.
     * <p/>
     * <b>NOTE:</b> Mapped (upgraded prefix) GOIDs created by this method are the actual, real GOIDs of entities that
     * have been upgraded to GOID from OID.  They can be used anywhere like any other GOID.
     * <p/>
     * Wrapped (prefix 0003) GOIDs created by this method are to be used for transitional purposes and
     * must not be persisted or externalized as identifiers for persisted entities -- doing so would defeat the purpose of using GOIDs.
     * It is OK to use such GOIDs as placeholders for references to entities that do not exist, as long as the
     * GOIDs never validly point at any real saved entities.
     * <p/>
     * You can use {@link GoidRange#WRAPPED_OID}'s {@link GoidRange#isInRange(com.l7tech.objectmodel.Goid)} method
     * to test whether a returned GOID has been wrapped vs mapped.
     *
     * @param entityType the entity type associated with the OID, or null to avoid checking for an upgraded prefix
     *                   and just always use the WRAPPED_OID prefix.
     * @param oid the objectid to wrap, or null to just return null.
     * @return a new Goid encoding this object ID with the upgraded prefix for this entity type, if available, or
     *         else with the WRAPPED_OID prefix, or null if oid was null or DefaultGoid if oid was -1.
     */
    public static Goid mapOid(@Nullable EntityType entityType, @Nullable Long oid) {
        if(oid == null)
            return null;
        else if(oid == -1)
            return PersistentEntity.DEFAULT_GOID;
        Long prefix = entityType == null ? null : getPrefix(entityType);
        if (prefix == null)
            prefix = GoidRange.WRAPPED_OID.getFirstHi();

        return new Goid( prefix, oid );
    }

    /**
     * Map a String to a Goid. This will convert a String id which either be a String representation of a Goid or a oid
     * into a Goid. If the String is already a goid hex string then it is converted to a Goid and that is returned. If
     * the String is a String representation of a long (oid) then the long is mapped using GoidUpgradeMapper#mapOid().
     * <p/>
     * If the given string is not a Hex goid representation or a long id then an IllegalArgumentException is thrown.
     *
     * @param entityType the entity type associated with the OID, or null to avoid checking for an upgraded prefix and
     *                   just always use the WRAPPED_OID prefix.
     * @param id         the id to parse, either a Goid hex String or a String long oid or null to just return null.
     * @return a new Goid encoding this object ID with the upgraded prefix for this entity type, if available, or else
     *         with the WRAPPED_OID prefix, or null if oid was null or DefaultGoid if oid was -1 or a Goid
     *         representation of the id if it was a Hex String.
     */
    public static Goid mapId(@Nullable EntityType entityType, @Nullable String id) {
        if(id == null)
            return null;
        try {
            Goid goid = new Goid(id);
            return goid;
        } catch (IllegalArgumentException e) {
            //do nothing, try it as an oid
        }
        try {
            return mapOid(entityType, Long.parseLong(id));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Id is neither a goid or an oid. ID: " + id);
        }
    }

    /**
     * Map a String to a Goid. This will convert a String id which either be a String representation of a Goid or a oid
     * into a Goid. If the String is already a goid hex string then it is converted to a Goid and that is returned. If
     * the String is a String representation of a long (oid) then the long is mapped using GoidUpgradeMapper#mapOidFromTableName().
     * <p/>
     * If the given string is not a Hex goid representation or a long id then an IllegalArgumentException is thrown.
     *
     * @param tableName  the table name associated with the OID, or null to avoid checking for an upgraded prefix and
     *                   just always use the WRAPPED_OID prefix.
     * @param id         the id to parse, either a Goid hex String or a String long oid or null to just return null.
     * @return a new Goid encoding this object ID with the upgraded prefix for this entity type, if available, or else
     *         with the WRAPPED_OID prefix, or null if oid was null or DefaultGoid if oid was -1 or a Goid
     *         representation of the id if it was a Hex String.
     */
    public static Goid mapId(@Nullable String tableName, @Nullable String id) {
        if(id == null)
            return null;
        try {
            Goid goid = new Goid(id);
            return goid;
        } catch (IllegalArgumentException e) {
            //do nothing, try it as an oid
        }
        try {
            return mapOidFromTableName(tableName, Long.parseLong(id));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Id is neither a goid or an oid. ID: " + id);
        }
    }

    /**
     * Map or wrap the specified legacy OID into a GOID, mapping it to the correct GOID for an entity from an updated
     * database, if applicable; otherwise, wrapping it as a Goid within the range reserved for a wrapped OID
     * (for temporary use within a Gateway upgraded from a pre-GOID database).
     * <p/>
     * <b>NOTE:</b> Mapped (upgraded prefix) GOIDs created by this method are the actual, real GOIDs of entities that
     * have been upgraded to GOID from OID.  They can be used anywhere like any other GOID.
     * <p/>
     * Wrapped (prefix 0003) GOIDs created by this method are to be used for transitional purposes and
     * must not be persisted or externalized as identifiers for persisted entities -- doing so would defeat the purpose of using GOIDs.
     * It is OK to use such GOIDs as placeholders for references to entities that do not exist, as long as the
     * GOIDs never validly point at any real saved entities.
     * <p/>
     * You can use {@link GoidRange#WRAPPED_OID}'s {@link GoidRange#isInRange(com.l7tech.objectmodel.Goid)} method
     * to test whether a returned GOID has been wrapped vs mapped.
     *
     * @param tableName the table name associated with the OID, or null to avoid checking for an upgraded prefix
     *                   and just always use the WRAPPED_OID prefix.
     * @param oid the objectid to wrap, or null to just return null.
     * @return a new Goid encoding this object ID with the upgraded prefix for this entity type, if available, or
     *         else with the WRAPPED_OID prefix, or null if oid was null.
     */
    public static Goid mapOidFromTableName(@NotNull String tableName, @Nullable Long oid) {
        Long prefix = getPrefix(tableName);
        if (prefix == null)
            prefix = GoidRange.WRAPPED_OID.getFirstHi();

        return oid == null
                ? null
                : new Goid( prefix, oid );
    }

    /**
     * Map or wrap legacy OIDs in the specified array to GOIDs, mapping them to the correct GOIDs for
     * entities from an updated database, if applicable; otherwise, wrapping them to GOIDs within the range reserved
     * for wrapped OIDs (for temporary use within a Gateway upgraded from a pre-GOID database). If a default oid is give
     * (-1) the default goid will be returned.
     * <p/>
     * <b>NOTE:</b> Mapped (upgraded prefix) GOIDs created by this method are the actual, real GOIDs of entities that
     * have been upgraded to GOID from OID.  They can be used anywhere like any other GOID.
     * <p/>
     * Wrapped (prefix 0003) GOIDs created by this method are to be used for transitional purposes and
     * must not be persisted or externalized as identifiers for persisted entities -- doing so would defeat the purpose of using GOIDs.
     * It is OK to use such GOIDs as placeholders for references to entities that do not exist, as long as the
     * GOIDs never validly point at any real saved entities.
     * <p/>
     * You can use {@link GoidRange#WRAPPED_OID}'s {@link GoidRange#isInRange(com.l7tech.objectmodel.Goid)} method
     * to test whether a returned GOID has been wrapped vs mapped.
     *
     * @param entityType the entity type associated with the OID, or null to avoid checking for an upgraded prefix
     *                   and just always use WRAPPED_OID prefixes.
     * @param oids the objectid array to wrap, or null to just return null.
     * @return an array of new Goid instances encoding the specified object ID with the upgraded prefix for this
     *         entity type, if available, or else with the WRAPPED_OID prefix, or null if oids was null or DefaultGoid if oid was -1..
     *         <p/>
     *         Elements of the returned array will be null if the correponding element in the input array was null.
     */
    public static Goid[] mapOids(@Nullable EntityType entityType, @Nullable Long[] oids) {
        if ( oids == null )
            return null;
        Long prefix = entityType == null ? null : getPrefix(entityType);
        if (prefix == null)
            prefix = GoidRange.WRAPPED_OID.getFirstHi();
        Goid[] goids = new Goid[oids.length];
        for ( int i = 0; i < oids.length; i++ ) {
            Long oid = oids[i];
            Goid goid;
            if(oid == null)
                goid = null;
            else if(oid == -1)
                goid = PersistentEntity.DEFAULT_GOID;
            else
                goid = new Goid( prefix, oid );
            goids[i] = goid;
        }
        return goids;
    }

    /**
     * @return a read-only version of the entire prefix map.  This is intended to be used only for
     *         making the SSM aware of the prefix map, so it can map any OIDs it encounters.
     */
    public static Map<String, Long> getTableNamePrefixMap() {
        return Collections.unmodifiableMap(tableNamePrefixMap);
    }

    /**
     * Initialize or replace the entire prefix map.
     *
     * @param prefixes new prefix map to replace existing version.
     */
    protected void setPrefixes( @NotNull Map<String, Long> prefixes ) {
        tableNamePrefixMap.clear();
        tableNamePrefixMap.putAll(prefixes);
        hasPrefixes = true;
    }

    /**
     * DO NOT USE!!! This is only to be used for testing purposed! To set a prefix in a unit test call
     * GoidUpgradeMapperTestUtil.addPrefix()
     *
     * @param tableName The table name for the prefix
     * @param prefix    The prefix value.
     */
    static void addPrefix(String tableName, long prefix) {
        hasPrefixes = true;
        tableNamePrefixMap.put(tableName, prefix);
    }

    /**
     * DO NOT USE!!! This is only to be used for testing purposes!  To clear all prefixes in a unit test
     * call GoidUpgradeMapperTestUtil.clearAllPrefixes().
     */
    static void clearAllPrefixes() {
        tableNamePrefixMap.clear();
    }
}
