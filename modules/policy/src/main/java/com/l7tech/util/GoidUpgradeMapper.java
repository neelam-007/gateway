package com.l7tech.util;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
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
        return hasPrefixes ? tableNamePrefixMap.get(tableName) : null;
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
        switch (entityType) {
            case JDBC_CONNECTION:
                return tableNamePrefixMap.get("jdbc_connection");
            case SAMPLE_MESSAGE:
                return tableNamePrefixMap.get("sample_messages");
            case CLUSTER_PROPERTY:
                return tableNamePrefixMap.get("cluster_properties");
            case EMAIL_LISTENER:
                return tableNamePrefixMap.get("email_listener");
            case GENERIC:
                return tableNamePrefixMap.get("generic_entity");
            case SSG_CONNECTOR:
                return tableNamePrefixMap.get("connector");
            case FIREWALL_RULE:
                return tableNamePrefixMap.get("firewall_rule");
            case ENCAPSULATED_ASSERTION:
                return tableNamePrefixMap.get("encapsulated_assertion");
            case JMS_CONNECTION:
                return tableNamePrefixMap.get("jms_connection");
            case JMS_ENDPOINT:
                return tableNamePrefixMap.get("jms_endpoint");
            case HTTP_CONFIGURATION:
                return tableNamePrefixMap.get("http_configuration");
            case SSG_ACTIVE_CONNECTOR:
                return tableNamePrefixMap.get("active_connector");
            case FOLDER:
                return tableNamePrefixMap.get("folder");
            case POLICY:
                return tableNamePrefixMap.get("policy");
            case POLICY_ALIAS:
                return tableNamePrefixMap.get("policy_alias");
            case POLICY_VERSION:
                return tableNamePrefixMap.get("policy_version");
            case SERVICE:
                return tableNamePrefixMap.get("published_service");
            case SERVICE_DOCUMENT:
                return tableNamePrefixMap.get("service_documents");
            case SERVICE_ALIAS:
                return tableNamePrefixMap.get("published_service_alias");
            case SERVICE_USAGE:
                return tableNamePrefixMap.get("service_usage");
            case UDDI_PROXIED_SERVICE_INFO:
                return tableNamePrefixMap.get("uddi_proxied_service_info");
            case UDDI_REGISTRY:
                return tableNamePrefixMap.get("uddi_registries");
            case UDDI_SERVICE_CONTROL:
                return tableNamePrefixMap.get("uddi_service_control");
            case ASSERTION_ACCESS:
                return tableNamePrefixMap.get("assertion_access");
            case AUDIT_ADMIN:
                return tableNamePrefixMap.get("audit_admin");
            case AUDIT_MESSAGE:
                return tableNamePrefixMap.get("audit_message");
            case AUDIT_RECORD:
                return tableNamePrefixMap.get("audit_main");
            case TRUSTED_CERT:
                return tableNamePrefixMap.get("trusted_cert");
            case CLUSTER_INFO:
                return tableNamePrefixMap.get("cluster_info");
            case CUSTOM_KEY_VALUE_STORE:
                return tableNamePrefixMap.get("custom_key_value_store");
            case ID_PROVIDER_CONFIG:
                return tableNamePrefixMap.get("identity_provider");
            case SSG_KEYSTORE:
                return tableNamePrefixMap.get("keystore_file");
            case SSG_KEY_METADATA:
                return tableNamePrefixMap.get("keystore_key_metadata");
            case LICENSE_DOCUMENT:
                return tableNamePrefixMap.get("license_document");
            case PASSWORD_POLICY:
                return tableNamePrefixMap.get("password_policy");
            case RBAC_ROLE:
                return tableNamePrefixMap.get("rbac_role");
            case RESOURCE_ENTRY:
                return tableNamePrefixMap.get("resource_entry");
            case REVOCATION_CHECK_POLICY:
                return tableNamePrefixMap.get("revocation_check_policy");
            case SECURE_PASSWORD:
                return tableNamePrefixMap.get("sample_messages");
            case SECURITY_ZONE:
                return tableNamePrefixMap.get("security_zone");
            case SITEMINDER_CONFIGURATION:
                return tableNamePrefixMap.get("siteminder_configuration");
            case TRUSTED_ESM:
                return tableNamePrefixMap.get("trusted_esm");
            case TRUSTED_ESM_USER:
                return tableNamePrefixMap.get("trusted_esm_user");
            default:
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
            return GoidEntity.DEFAULT_GOID;
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
                goid = GoidEntity.DEFAULT_GOID;
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
