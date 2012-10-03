package com.l7tech.portal.metrics;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.sql.*;

public abstract class AbstractPortalMetricsUtility {
    private static final Logger LOGGER = Logger.getLogger(AbstractPortalMetricsUtility.class);
    static final int FINE_RESOLUTION = 0;
    static final int CUSTOM_RESOLUTION = 3;
    // table names
    static final String SERVICE_METRICS = "service_metrics";
    static final String SERVICE_METRICS_DETAILS = "service_metrics_details";
    static final String MAPPING_KEYS = "message_context_mapping_keys";
    static final String MAPPING_VALUES = "message_context_mapping_values";
    static final String GENERIC_ENTITY = "generic_entity";

    static final String PUBLISHED_SERVICE = "published_service";
    // insert queries
    static final String INSERT_SERVICE_METRICS_QUERY = "INSERT INTO " + SERVICE_METRICS +
            " (nodeid,published_service_oid,resolution,period_start,start_time,interval_size,end_time,service_state," +
            "attempted,authorized,completed,back_min,back_max,back_sum,front_min,front_max,front_sum,uuid) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static final String INSERT_SERVICE_METRICS_DETAILS_QUERY = "INSERT INTO " + SERVICE_METRICS_DETAILS +
            " (service_metrics_oid,mapping_values_oid,attempted,authorized,completed,back_min,back_max,back_sum," +
            "front_min,front_max,front_sum) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    // IGNORE is required to protect against duplicate inserts however this is MySQL specific
    static String INSERT_MESSAGE_CONTEXT_MAPPING_VALUES_QUERY = "INSERT IGNORE INTO " + MAPPING_VALUES + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static String INSERT_MESSAGE_CONTEXT_MAPPING_KEYS_QUERY = "INSERT IGNORE INTO " + MAPPING_KEYS + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    static String INSERT_PUBLISHED_SERVICE_QUERY = "INSERT IGNORE INTO " + PUBLISHED_SERVICE + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    // select queries
    public static final String SELECT_MAPPING_KEY = "SELECT * FROM " + MAPPING_KEYS + " WHERE OBJECTID=?";
    public static final String SELECT_MAPPING_VALUE = "SELECT * FROM " + MAPPING_VALUES + " WHERE objectid=?";
    public static final String SELECT_PUBLISHED_SERVICE = "SELECT * FROM " + PUBLISHED_SERVICE + " WHERE objectid=?";

    protected final DatabaseInfo sourceDatabaseInfo;
    protected final DatabaseInfo destDatabaseInfo;

    public AbstractPortalMetricsUtility(final DatabaseInfo sourceDatabaseInfo,final DatabaseInfo destDatabaseInfo) {
        Validate.notNull(sourceDatabaseInfo, "Source database info must be specified.");
        Validate.notNull(destDatabaseInfo, "Destination database info must be specified.");
        this.sourceDatabaseInfo = sourceDatabaseInfo;
        this.destDatabaseInfo = destDatabaseInfo;
    }

    protected Long getNullableLong(final ResultSet resultSet, final String columnName) throws SQLException {
        Long result = null;
        final String valueAsString = resultSet.getString(columnName);
        if (valueAsString != null && !valueAsString.isEmpty()) {
            try {
                result = Long.valueOf(valueAsString);
            } catch (final NumberFormatException e) {
                LogUtil.logError(LOGGER, "Unable to parse long from " + valueAsString, e);
            }
        }
        return result;
    }

    protected Integer getNullableInt(final ResultSet resultSet, final String columnName) throws SQLException {
        Integer result = null;
        result = parseStringAsInt(resultSet.getString(columnName));
        return result;
    }

    protected Integer getNullableInt(final ResultSet resultSet, final int columnIndex) throws SQLException {
        Integer result = null;
        result = parseStringAsInt(resultSet.getString(columnIndex));
        return result;
    }

    protected void closeConnection(final Connection connection) throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Ensures that even if first connection fails to close, an attempt to close the second will be made.
     */
    protected void closeConnections(final Connection first, final Connection second) throws SQLException {
        try {
            closeConnection(first);
        } finally {
            closeConnection(second);
        }
    }

    private Integer parseStringAsInt(final String valueAsString) {
        Integer result = null;
        if (valueAsString != null && !valueAsString.isEmpty()) {
            try {
                result = Integer.valueOf(valueAsString);
            } catch (final NumberFormatException e) {
                LogUtil.logError(LOGGER, "Unable to parse integer from " + valueAsString, e);
            }
        }
        return result;
    }

    protected int copyMappingKey(final Connection sourceConnection, final Connection destConnection, final long mappingKeysId) throws SQLException {
        final PreparedStatement selectMappingKey = sourceConnection.prepareStatement(SELECT_MAPPING_KEY);
        selectMappingKey.setLong(1, mappingKeysId);
        final ResultSet referencedMappingKey = selectMappingKey.executeQuery();
        if (referencedMappingKey.next()) {
            final int version = referencedMappingKey.getInt("VERSION");
            final String kdigested = referencedMappingKey.getString("digested");
            final String mapping1Type = referencedMappingKey.getString("MAPPING1_TYPE");
            final String mapping1Key = referencedMappingKey.getString("MAPPING1_KEY");
            final String mapping2Type = referencedMappingKey.getString("MAPPING2_TYPE");
            final String mapping2Key = referencedMappingKey.getString("MAPPING2_KEY");
            final String mapping3Type = referencedMappingKey.getString("MAPPING3_TYPE");
            final String mapping3Key = referencedMappingKey.getString("MAPPING3_KEY");
            final String mapping4Type = referencedMappingKey.getString("MAPPING4_TYPE");
            final String mapping4Key = referencedMappingKey.getString("MAPPING4_KEY");
            final String mapping5Type = referencedMappingKey.getString("MAPPING5_TYPE");
            final String mapping5Key = referencedMappingKey.getString("MAPPING5_KEY");
            final Long kCreateTime = getNullableLong(referencedMappingKey, "CREATE_TIME");
            final PreparedStatement insertMappingKey = destConnection.prepareStatement(INSERT_MESSAGE_CONTEXT_MAPPING_KEYS_QUERY);
            int i = 0;
            insertMappingKey.setLong(++i, mappingKeysId);
            insertMappingKey.setInt(++i, version);
            insertMappingKey.setString(++i, kdigested);
            insertMappingKey.setString(++i, mapping1Type);
            insertMappingKey.setString(++i, mapping1Key);
            insertMappingKey.setString(++i, mapping2Type);
            insertMappingKey.setString(++i, mapping2Key);
            insertMappingKey.setString(++i, mapping3Type);
            insertMappingKey.setString(++i, mapping3Key);
            insertMappingKey.setString(++i, mapping4Type);
            insertMappingKey.setString(++i, mapping4Key);
            insertMappingKey.setString(++i, mapping5Type);
            insertMappingKey.setString(++i, mapping5Key);
            insertMappingKey.setObject(++i, kCreateTime, Types.BIGINT);
            final int keyRows = insertMappingKey.executeUpdate();
            if (keyRows == 0) {
                LOGGER.info("Mapping key " + mappingKeysId + " already exists in destination database.");
            } else {
                LOGGER.info("Copied " + keyRows + " row(s) to the " + MAPPING_KEYS + " table.");
            }
            insertMappingKey.close();
            return keyRows;
        }
        LOGGER.info("Copy mapping key skipped. Mapping key " + mappingKeysId + " does not exist in source database.");
        selectMappingKey.close();
        return 0;
    }

    protected int copyMappingValue(final Connection sourceConnection, final Connection destConnection, final long mappingValuesId) throws SQLException {
        final PreparedStatement selectMappingValue = sourceConnection.prepareStatement(SELECT_MAPPING_VALUE);
        selectMappingValue.setLong(1, mappingValuesId);
        final ResultSet referencedMappingValue = selectMappingValue.executeQuery();
        if (referencedMappingValue.next()) {
            // foreign key
            final long mappingKeysId = referencedMappingValue.getLong("mapping_keys_oid");
            copyMappingKey(sourceConnection, destConnection, mappingKeysId);

            final String digested = referencedMappingValue.getString("digested");
            final long providerId = referencedMappingValue.getLong("auth_user_provider_id");
            final String authUserId = referencedMappingValue.getString("auth_user_id");
            final String authUserUniqueId = referencedMappingValue.getString("AUTH_USER_UNIQUE_ID");
            final String serviceOperation = referencedMappingValue.getString("SERVICE_OPERATION");
            final String mapping1Value = referencedMappingValue.getString("MAPPING1_VALUE");
            final String mapping2Value = referencedMappingValue.getString("MAPPING2_VALUE");
            final String mapping3Value = referencedMappingValue.getString("MAPPING3_VALUE");
            final String mapping4Value = referencedMappingValue.getString("MAPPING4_VALUE");
            final String mapping5Value = referencedMappingValue.getString("MAPPING5_VALUE");
            final Long createTime = getNullableLong(referencedMappingValue, "CREATE_TIME");

            final PreparedStatement insertMappingValue = destConnection.prepareStatement(INSERT_MESSAGE_CONTEXT_MAPPING_VALUES_QUERY);
            int k = 0;
            insertMappingValue.setLong(++k, mappingValuesId);
            insertMappingValue.setString(++k, digested);
            insertMappingValue.setLong(++k, mappingKeysId);
            insertMappingValue.setLong(++k, providerId);
            insertMappingValue.setString(++k, authUserId);
            insertMappingValue.setString(++k, authUserUniqueId);
            insertMappingValue.setString(++k, serviceOperation);
            insertMappingValue.setString(++k, mapping1Value);
            insertMappingValue.setString(++k, mapping2Value);
            insertMappingValue.setString(++k, mapping3Value);
            insertMappingValue.setString(++k, mapping4Value);
            insertMappingValue.setString(++k, mapping5Value);
            insertMappingValue.setObject(++k, createTime, Types.BIGINT);
            final int valueRows = insertMappingValue.executeUpdate();
            if (valueRows == 0) {
                LOGGER.info("Mapping Value " + mappingValuesId + " already exists in destination database.");
            } else {
                LOGGER.info("Copied " + valueRows + " row(s) to the " + MAPPING_VALUES + " table.");
            }
            insertMappingValue.close();
            return valueRows;
        }
        LOGGER.info("Copy mapping value skipped. Mapping value " + mappingValuesId + " does not exist in source database.");
        selectMappingValue.close();
        return 0;
    }

    protected int copyPublishedService(final Connection sourceConnection, final Connection destConnection, final Long serviceId, final String uuid) throws SQLException {
        final PreparedStatement selectPublishedService = sourceConnection.prepareStatement(SELECT_PUBLISHED_SERVICE);
        selectPublishedService.setLong(1, serviceId);
        final ResultSet foundPublishedService = selectPublishedService.executeQuery();
        if (foundPublishedService.next()) {
            final int version = foundPublishedService.getInt("version");
            final String name = foundPublishedService.getString("NAME");
            final String policyXml = foundPublishedService.getString("POLICY_XML");
            final Long policyId = getNullableLong(foundPublishedService, "POLICY_OID");
            final String wsdlUrl = foundPublishedService.getString("WSDL_URL");
            final String wsdlXml = foundPublishedService.getString("WSDL_XML");
            final int disabled = foundPublishedService.getInt("DISABLED");
            final int soap = foundPublishedService.getInt("SOAP");
            final int internal = foundPublishedService.getInt("INTERNAL");
            final String routingUri = foundPublishedService.getString("ROUTING_URI");
            final String defaultRoutingUrl = foundPublishedService.getString("DEFAULT_ROUTING_URL");
            final String httpMethods = foundPublishedService.getString("HTTP_METHODS");
            final int laxResolution = foundPublishedService.getInt("LAX_RESOLUTION");
            final int wssProcessing = foundPublishedService.getInt("WSS_PROCESSING");
            final int tracing = foundPublishedService.getInt("TRACING");
            final Long folderId = getNullableLong(foundPublishedService, "FOLDER_OID");
            final String soapVersion = foundPublishedService.getString("SOAP_VERSION");

            final PreparedStatement insertPublishedService = destConnection.prepareStatement(INSERT_PUBLISHED_SERVICE_QUERY);
            int i = 0;
            insertPublishedService.setLong(++i, serviceId);
            insertPublishedService.setInt(++i, version);
            insertPublishedService.setString(++i, name);
            insertPublishedService.setString(++i, policyXml);
            insertPublishedService.setObject(++i, policyId, Types.BIGINT);
            insertPublishedService.setString(++i, wsdlUrl);
            insertPublishedService.setString(++i, wsdlXml);
            insertPublishedService.setInt(++i, disabled);
            insertPublishedService.setInt(++i, soap);
            insertPublishedService.setInt(++i, internal);
            insertPublishedService.setString(++i, routingUri);
            insertPublishedService.setString(++i, defaultRoutingUrl);
            insertPublishedService.setString(++i, httpMethods);
            insertPublishedService.setInt(++i, laxResolution);
            insertPublishedService.setInt(++i, wssProcessing);
            insertPublishedService.setInt(++i, tracing);
            insertPublishedService.setObject(++i, folderId, Types.BIGINT);
            insertPublishedService.setString(++i, soapVersion);
            insertPublishedService.setString(++i, uuid);
            final int rows = insertPublishedService.executeUpdate();
            if (rows == 0) {
                LOGGER.info("Published service " + serviceId + " already exists in destination database.");
            } else {
                LOGGER.info("Copied " + rows + " row(s) to the " + PUBLISHED_SERVICE + " table.");
            }
            insertPublishedService.close();
            return rows;
        }
        LOGGER.info("Copy published service skipped. Published service " + serviceId + " does not exist in source database.");
        selectPublishedService.close();
        return 0;
    }
}
