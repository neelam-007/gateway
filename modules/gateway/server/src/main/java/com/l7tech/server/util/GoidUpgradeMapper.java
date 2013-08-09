package com.l7tech.server.util;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ResourceUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to retrieve the prefixes that were generated for existing entities during an 8.0.0 upgrade. If
 * this gateway never went through an 8.0.0 upgrade no prefixes will have been generated and the get prefix methods will
 * return null.
 *
 * @author Victor Kazakov
 */
public class GoidUpgradeMapper extends ApplicationObjectSupport implements InitializingBean {
    private static final Logger logger = Logger.getLogger(GoidUpgradeMapper.class.getName());

    public static final String GOID_UPGRADE_MAP_TABLE = "goid_upgrade_map";
    public static final String PREFIX_COLUMN = "prefix";
    public static final String TABLE_NAME_COLUMN = "table_name";
    public static final String GET_BY_ENTITY_TYPE_STMT = "SELECT " + PREFIX_COLUMN + ", " + TABLE_NAME_COLUMN + " FROM " + GOID_UPGRADE_MAP_TABLE;

    private static final Map<String, Long> tableNamePrefixMap = new HashMap<>();
    private static boolean hasPrefixes = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        ApplicationContext appCtx = getApplicationContext();

        SessionFactory sf = ((SessionFactory) appCtx.getBean("sessionFactory"));
        Session session;
        session = sf.openSession();

        //This will load any prefixes generated during an upgrade by looking at the goid_upgrade_map table
        //If the table doesn't exist no error if thrown and no prefixes are loaded.
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                Statement st = null;
                ResultSet rs = null;

                try {
                    st = connection.createStatement();
                    try {
                        rs = st.executeQuery(GET_BY_ENTITY_TYPE_STMT);
                        logger.log(Level.FINE, "Found Goid prefixes generated by an 8.0.0 upgrade, populating the GoidUpgradeMapper.");
                    } catch (SQLException e) {
                        // Do nothing here this means that the prefix table was never created and so this gateway never
                        // went through the 8.0 (Halibut) upgrade process
                        logger.log(Level.FINE, "Did not find any Goid prefixes generated by an 8.0.0 upgrade.");
                        return;
                    }
                    //Let errors be thrown from below. It is unexpected if they do get thrown at this point.
                    while (rs.next()) {
                        //we have a table, now check the contents
                        Long prefix = rs.getLong(PREFIX_COLUMN);
                        String entityType = rs.getString(TABLE_NAME_COLUMN);
                        tableNamePrefixMap.put(entityType, prefix);
                    }
                    hasPrefixes = true;
                } finally {
                    ResourceUtils.closeQuietly(rs);
                    ResourceUtils.closeQuietly(st);
                }
            }
        });
    }

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
        return prefixFound != null ? prefixFound == prefix : false;
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
     * @param entityType The entity type to get the prefix for.
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
     * DO NOT USE!!! This is only to be used for testing purposed! To set a prefix in a unit test call
     * GoidUpgradeMapperTestUtil.addPrefix()
     *
     * @param tableName The table name for the prefix
     * @param prefix    The prefix value.
     */
    protected static void addPrefix(String tableName, long prefix) {
        tableNamePrefixMap.put(tableName, prefix);
    }
}
