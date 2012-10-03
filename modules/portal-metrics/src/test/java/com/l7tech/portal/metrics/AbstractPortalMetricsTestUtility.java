package com.l7tech.portal.metrics;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

public abstract class AbstractPortalMetricsTestUtility {
    protected static final String USERNAME = "sa";
    protected static final String PASSWORD = "";
    protected static final String SOURCE_DB = "jdbc:hsqldb:mem:sourcedb";
    protected static final String DEST_DB = "jdbc:hsqldb:mem:destdb";
    protected Map<Long, String> portalManagedServiceIds;
    protected Connection sourceConnection;
    protected Connection destConnection;

    public void setupParent() throws Exception {
        portalManagedServiceIds = new HashMap<Long, String>();

        sourceConnection = DriverManager.getConnection(SOURCE_DB);

        createTables(sourceConnection);
        destConnection = DriverManager.getConnection(DEST_DB);

        createTables(destConnection);
    }

    public void teardownParent() throws Exception {
        dropTables(sourceConnection);
        sourceConnection.close();
        dropTables(destConnection);
        destConnection.close();
    }

    protected void createTables(final Connection connection) throws SQLException {
        createPublishedServiceTable(connection);
        createServiceMetricsTable(connection);
        createMappingKeysTable(connection);
        createMappingValuesTable(connection);
        createServiceMetricDetailsTable(connection);
        createGenericEntityTable(connection);
    }

    private void createGenericEntityTable(final Connection connection) throws SQLException {
        connection.createStatement().execute("CREATE TABLE " + AbstractPortalMetricsUtility.GENERIC_ENTITY + " (\n" +
                "  objectid bigint NOT NULL,\n" +
                "  version int NOT NULL,\n" +
                "  name varchar(255),\n" +
                "  description varchar(255),\n" +
                "  classname varchar(255) NOT NULL,\n" +
                "  enabled TINYINT DEFAULT 1 NOT NULL,\n" +
                "  value_xml varchar(255),\n" +
                "  PRIMARY KEY (objectid),\n" +
                "  UNIQUE (classname, name)\n" +
                ");");
    }

    private void createPublishedServiceTable(Connection connection) throws SQLException {
        connection.createStatement().execute("CREATE TABLE " + AbstractPortalMetricsUtility.PUBLISHED_SERVICE + " (\n" +
                "  objectid bigint NOT NULL,\n" +
                "  version int NOT NULL,\n" +
                "  name varchar(255) NOT NULL,\n" +
                "  policy_xml varchar(255),\n" +
                "  policy_oid bigint default NULL,\n" +
                "  wsdl_url varchar(255),\n" +
                "  wsdl_xml varchar(255),\n" +
                "  disabled TINYINT DEFAULT 0 NOT NULL,\n" +
                "  soap TINYINT DEFAULT 1 NOT NULL,\n" +
                "  internal TINYINT DEFAULT 0 NOT NULL,\n" +
                "  routing_uri varchar(128),\n" +
                "  default_routing_url varchar(4096),\n" +
                "  http_methods varchar(255),\n" +
                "  lax_resolution TINYINT DEFAULT 0 NOT NULL,\n" +
                "  wss_processing TINYINT DEFAULT 1 NOT NULL,\n" +
                "  tracing TINYINT DEFAULT 0 NOT NULL,\n" +
                "  folder_oid bigint,\n" +
                "  soap_version VARCHAR(20) DEFAULT 'UNKNOWN',\n" +
                "  uuid VARCHAR(48) NULL ,\n" +
                "  PRIMARY KEY (objectid)\n" +
                ");");
    }

    private void createServiceMetricsTable(Connection connection) throws SQLException {
        connection.createStatement().execute("CREATE TABLE " + AbstractPortalMetricsUtility.SERVICE_METRICS + " (\n" +
                "  objectid bigint NOT NULL IDENTITY,\n" +
                "  nodeid VARCHAR(32) NOT NULL,\n" +
                "  published_service_oid BIGINT NOT NULL,\n" +
                "  resolution INTEGER NOT NULL,\n" +
                "  period_start BIGINT NOT NULL,\n" +
                "  start_time BIGINT NOT NULL,\n" +
                "  interval_size INTEGER NOT NULL,\n" +
                "  end_time BIGINT NOT NULL,\n" +
                "  attempted INTEGER NOT NULL,\n" +
                "  authorized INTEGER NOT NULL,\n" +
                "  completed INTEGER NOT NULL,\n" +
                "  back_min INTEGER,\n" +
                "  back_max INTEGER,\n" +
                "  back_sum INTEGER NOT NULL,\n" +
                "  front_min INTEGER,\n" +
                "  front_max INTEGER,\n" +
                "  front_sum INTEGER NOT NULL,\n" +
                "  service_state VARCHAR(16),\n" +
                "  uuid VARCHAR(48) NULL,\n" +
                "  UNIQUE (nodeid, published_service_oid, resolution, period_start)\n" +
                ");");
    }

    private void createMappingKeysTable(Connection connection) throws SQLException {
        connection.createStatement().execute("CREATE TABLE " + AbstractPortalMetricsUtility.MAPPING_KEYS + " (\n" +
                "  objectid bigint NOT NULL,\n" +
                "  version int NOT NULL,\n" +
                "  digested char(36) NOT NULL,\n" +
                "  mapping1_type varchar(36),\n" +
                "  mapping1_key varchar(128),\n" +
                "  mapping2_type varchar(36),\n" +
                "  mapping2_key varchar(128),\n" +
                "  mapping3_type varchar(36),\n" +
                "  mapping3_key varchar(128),\n" +
                "  mapping4_type varchar(36),\n" +
                "  mapping4_key varchar(128),\n" +
                "  mapping5_type varchar(36),\n" +
                "  mapping5_key varchar(128),\n" +
                "  create_time bigint,\n" +
                "  PRIMARY KEY (objectid),\n" +
                ");");
    }

    private void createMappingValuesTable(Connection connection) throws SQLException {
        connection.createStatement().execute("CREATE TABLE " + AbstractPortalMetricsUtility.MAPPING_VALUES + " (\n" +
                "  objectid bigint NOT NULL,\n" +
                "  digested char(36) NOT NULL,\n" +
                "  mapping_keys_oid bigint NOT NULL,\n" +
                "  auth_user_provider_id bigint,\n" +
                "  auth_user_id varchar(255),\n" +
                "  auth_user_unique_id varchar(255),\n" +
                "  service_operation varchar(255),\n" +
                "  mapping1_value varchar(255),\n" +
                "  mapping2_value varchar(255),\n" +
                "  mapping3_value varchar(255),\n" +
                "  mapping4_value varchar(255),\n" +
                "  mapping5_value varchar(255),\n" +
                "  create_time bigint,\n" +
                "  PRIMARY KEY  (objectid),\n" +
                "  FOREIGN KEY (mapping_keys_oid) REFERENCES " + AbstractPortalMetricsUtility.MAPPING_KEYS + " (objectid)\n" +
                ");");
    }

    private void createServiceMetricDetailsTable(Connection connection) throws SQLException {
        connection.createStatement().execute("CREATE TABLE " + AbstractPortalMetricsUtility.SERVICE_METRICS_DETAILS + " (\n" +
                "  service_metrics_oid BIGINT NOT NULL,\n" +
                "  mapping_values_oid BIGINT NOT NULL,\n" +
                "  attempted INTEGER NOT NULL,\n" +
                "  authorized INTEGER NOT NULL,\n" +
                "  completed INTEGER NOT NULL,\n" +
                "  back_min INTEGER,\n" +
                "  back_max INTEGER,\n" +
                "  back_sum INTEGER NOT NULL,\n" +
                "  front_min INTEGER,\n" +
                "  front_max INTEGER,\n" +
                "  front_sum INTEGER NOT NULL,\n" +
                "  uuid VARCHAR(48) NULL,\n" +
                "  PRIMARY KEY (service_metrics_oid, mapping_values_oid),\n" +
                "  CONSTRAINT fk_sm FOREIGN KEY (service_metrics_oid) REFERENCES " + AbstractPortalMetricsUtility.SERVICE_METRICS + " (objectid) ON DELETE CASCADE,\n" +
                "  CONSTRAINT fk_mv FOREIGN KEY (mapping_values_oid) REFERENCES " + AbstractPortalMetricsUtility.MAPPING_VALUES + " (objectid)\n" +
                ");");
    }

    protected void dropTables(final Connection connection) throws Exception {
        connection.createStatement().execute("DROP TABLE " + AbstractPortalMetricsUtility.GENERIC_ENTITY);
        connection.createStatement().execute("DROP TABLE " + AbstractPortalMetricsUtility.SERVICE_METRICS_DETAILS);
        connection.createStatement().execute("DROP TABLE " + AbstractPortalMetricsUtility.MAPPING_VALUES);
        connection.createStatement().execute("DROP TABLE " + AbstractPortalMetricsUtility.MAPPING_KEYS);
        connection.createStatement().execute("DROP TABLE " + AbstractPortalMetricsUtility.SERVICE_METRICS);
        connection.createStatement().execute("DROP TABLE " + AbstractPortalMetricsUtility.PUBLISHED_SERVICE);
    }

    protected void insertGenericEntity(final Connection connection, final long objectId, final String name, final String description, final String classname) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement("insert into " + AbstractPortalMetricsUtility.GENERIC_ENTITY + " values(?,?,?,?,?,?,?)");
        int i = 0;
        preparedStatement.setLong(++i, objectId);
        preparedStatement.setInt(++i, 1);
        preparedStatement.setString(++i, name);
        preparedStatement.setString(++i, description);
        preparedStatement.setString(++i, classname);
        preparedStatement.setInt(++i, 1);
        preparedStatement.setString(++i, "<thexml/>");
        preparedStatement.execute();
    }

    protected void insertMappingKey(final Connection connection, final long objectId) throws Exception {
        insertMappingKey(connection, objectId, 1L);
    }

    protected void insertMappingKey(final Connection connection, final long objectId, final Long createTime) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(AbstractPortalMetricsUtility.INSERT_MESSAGE_CONTEXT_MAPPING_KEYS_QUERY);
        int paramCount = 0;
        insertStatement.setLong(++paramCount, objectId); //  object id
        insertStatement.setInt(++paramCount, 1); // version
        insertStatement.setString(++paramCount, "d"); // digested
        insertStatement.setString(++paramCount, null); // mapping1 type
        insertStatement.setString(++paramCount, null); // mapping1 key
        insertStatement.setString(++paramCount, null); // mapping2 type
        insertStatement.setString(++paramCount, null); // mapping2 key
        insertStatement.setString(++paramCount, null); // mapping3 type
        insertStatement.setString(++paramCount, null); // mapping3 key
        insertStatement.setString(++paramCount, null); // mapping4 type
        insertStatement.setString(++paramCount, null); // mapping4 key
        insertStatement.setString(++paramCount, null); // mapping5 type
        insertStatement.setString(++paramCount, null); // mapping5 key
        insertStatement.setObject(++paramCount, createTime, Types.BIGINT); // create time
        insertStatement.execute();
    }

    protected void insertMappingValue(final Connection connection, final long objectId, final long mappingKeysOid) throws Exception {
        insertMappingValue(connection, objectId, mappingKeysOid, 1L);
    }

    protected void insertMappingValue(final Connection connection, final long objectId, final long mappingKeysOid, final Long createTime) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(AbstractPortalMetricsUtility.INSERT_MESSAGE_CONTEXT_MAPPING_VALUES_QUERY);
        int paramCount = 0;
        insertStatement.setLong(++paramCount, objectId); // object id
        insertStatement.setString(++paramCount, "d"); // digested
        insertStatement.setLong(++paramCount, mappingKeysOid); // mapping keys oid
        insertStatement.setLong(++paramCount, 1); // auth user provider id
        insertStatement.setString(++paramCount, "test"); // auth user id
        insertStatement.setString(++paramCount, "unique"); // auth user unique id
        insertStatement.setString(++paramCount, "op"); // service operation
        insertStatement.setString(++paramCount, null); // mapping1 value
        insertStatement.setString(++paramCount, null); // mapping2 value
        insertStatement.setString(++paramCount, null); // mapping3 value
        insertStatement.setString(++paramCount, null); // mapping4 value
        insertStatement.setString(++paramCount, null); // mapping5 value
        insertStatement.setObject(++paramCount, createTime); // create time
        insertStatement.execute();
    }

    protected void insertServiceMetricDetail(final Connection connection, final long serviceMetricsOid, final long mappingValuesOid) throws Exception {
        insertServiceMetricDetail(connection, serviceMetricsOid, mappingValuesOid, 1, 1, 1, 1);
    }

    protected void insertServiceMetricDetail(final Connection connection, final long serviceMetricsOid, final long mappingValuesOid, final Integer backMin, final Integer backMax, final Integer frontMin, final Integer frontMax) throws Exception {
        insertServiceMetricDetail(connection, serviceMetricsOid, mappingValuesOid, 1, 1, 1, backMin, backMax, 1, frontMin, frontMax, 1);
    }

    protected void insertServiceMetricDetail(final Connection connection, final long serviceMetricsOid, final long mappingValuesOid, final Integer attempted, final Integer authorized, final Integer completed, final Integer backMin, final Integer backMax, final Integer backSum, final Integer frontMin, final Integer frontMax, final Integer frontSum) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(AbstractPortalMetricsUtility.INSERT_SERVICE_METRICS_DETAILS_QUERY);
        insertStatement.setLong(1, serviceMetricsOid); // service metrics oid
        insertStatement.setLong(2, mappingValuesOid); // mapping values oid
        int next = setBaseMetricFields(insertStatement, 3, attempted, authorized, completed, backMin, backMax, backSum, frontMin, frontMax, frontSum);
        insertStatement.execute();
    }

    protected long insertServiceMetric(final Connection connection, final long serviceId, final long periodStart) throws Exception {
        return insertServiceMetric(connection, serviceId, periodStart, 1, 1, 1, 1);
    }

    protected long insertServiceMetric(final Connection connection, final long serviceId, final long periodStart, final String uuid) throws Exception {
        return insertServiceMetric(connection, serviceId, "node", periodStart, periodStart, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, uuid);
    }

    protected long insertServiceMetric(final Connection connection, final long serviceId, final long periodStart, final Integer backMin, final Integer backMax, final Integer frontMin, final Integer frontMax) throws Exception {
        return insertServiceMetric(connection, serviceId, "node", periodStart, periodStart, 1, 1, 1, 1, 1, 1, backMin, backMax, 1, frontMin, frontMax, 1, "UUID1234");
    }

    protected long insertServiceMetric(final Connection connection, final long serviceId, final String nodeId, final long periodStart, final long startTime, final long endTime, final int resolution, final int intervalSize, final Integer attempted, final Integer authorized, final Integer completed, final Integer backMin, final Integer backMax, final Integer backSum, final Integer frontMin, final Integer frontMax, final Integer frontSum, final String uuid) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(AbstractPortalMetricsUtility.INSERT_SERVICE_METRICS_QUERY, Statement.RETURN_GENERATED_KEYS);
        insertStatement.setString(1, nodeId); // node id
        insertStatement.setLong(2, serviceId); // service id
        insertStatement.setInt(3, resolution); // resolution
        insertStatement.setLong(4, periodStart); // period start
        insertStatement.setLong(5, startTime); // start time
        insertStatement.setInt(6, intervalSize); // interval size
        insertStatement.setLong(7, endTime); // end time
        insertStatement.setString(8, "ENABLED"); // service state
        int next = setBaseMetricFields(insertStatement, 9, attempted, authorized, completed, backMin, backMax, backSum, frontMin, frontMax, frontSum);
        insertStatement.setString(++next, uuid); // service state
        insertStatement.execute();
        final ResultSet generatedKey = insertStatement.getGeneratedKeys();
        generatedKey.next();
        return generatedKey.getLong(1);
    }

    protected void insertPublishedService(final Connection connection, final long serviceId) throws Exception {
        insertPublishedService(connection, serviceId, 1L, 1L, false, "UUID1234");
    }

    protected void insertPublishedService(final Connection connection, final long serviceId, final String uuid) throws Exception {
        insertPublishedService(connection, serviceId, 1L, 1L, false, uuid);
    }

    protected void insertPublishedService(final Connection connection, final long serviceId, final boolean disabled) throws Exception {
        insertPublishedService(connection, serviceId, 1L, 1L, disabled, "UUID1234");
    }

    protected void insertPublishedService(final Connection connection, final long serviceId, final Long policyId, final Long folderId, final boolean disabled, final String uuid) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(AbstractPortalMetricsUtility.INSERT_PUBLISHED_SERVICE_QUERY);
        int count = 0;
        insertStatement.setLong(++count, serviceId); // object id
        insertStatement.setInt(++count, 1); //version
        insertStatement.setString(++count, "test service"); //name
        insertStatement.setString(++count, "xml"); //policy xml
        insertStatement.setObject(++count, policyId, Types.BIGINT); // policy oid - nullable
        insertStatement.setString(++count, "wsdl url"); // wsdl url
        insertStatement.setString(++count, "xml"); // wsdl xml
        insertStatement.setInt(++count, disabled ? 1 : 0); // disabled
        insertStatement.setInt(++count, 1); // soap
        insertStatement.setInt(++count, 0); // internal
        insertStatement.setString(++count, "routing uri"); // routing uri
        insertStatement.setString(++count, "default routing url"); // default routing url
        insertStatement.setString(++count, "POST"); // http methods
        insertStatement.setInt(++count, 0); // lax resolution
        insertStatement.setInt(++count, 1); // wss processing
        insertStatement.setInt(++count, 0); // tracing
        insertStatement.setObject(++count, folderId, Types.BIGINT); // folder oid - nullable
        insertStatement.setString(++count, "soap version"); // soap version
        insertStatement.setString(++count, uuid); // UUID
        insertStatement.execute();
    }

    private void setBaseMetricFields(final PreparedStatement insertStatement, final int startingIndex, final Integer backMin, final Integer backMax, final Integer frontMin, final Integer frontMax) throws SQLException {
        setBaseMetricFields(insertStatement, startingIndex, 1, 1, 1, backMin, backMax, 1, frontMin, frontMax, 1);
    }

    private int setBaseMetricFields(final PreparedStatement insertStatement, final int startingIndex, final Integer attempted, final Integer authorized, final Integer completed, final Integer backMin, final Integer backMax, final Integer backSum, final Integer frontMin, final Integer frontMax, final Integer frontSum) throws SQLException {
        int index = startingIndex;
        insertStatement.setInt(index, attempted); // attempted
        insertStatement.setInt(++index, authorized); // authorized
        insertStatement.setInt(++index, completed); // completed
        insertStatement.setObject(++index, backMin); // back min - nullable
        insertStatement.setObject(++index, backMax); // back max - nullable
        insertStatement.setInt(++index, backSum); // back sum
        insertStatement.setObject(++index, frontMin); // front min - nullable
        insertStatement.setObject(++index, frontMax); // front max - nullable
        insertStatement.setInt(++index, frontSum); // front sum
        return index;
    }

    protected void assertNumRows(final Connection connection, final int expectedNumNonMappingRows, final int expectedNumMappingRows, final int expectedNumPublishedServices) throws Exception {
        assertNumRows(connection, expectedNumNonMappingRows, expectedNumMappingRows, expectedNumMappingRows, expectedNumPublishedServices);
    }


    protected void assertNumRows(final Connection connection, final int expectedNumNonMappingRows, final int expectedNumMappingKeyRows, final int expectedNumMappingValueRows, final int expectedNumPublishedServices) throws Exception {
        assertEquals(expectedNumNonMappingRows, getNumRows(connection, AbstractPortalMetricsUtility.SERVICE_METRICS));
        assertEquals(expectedNumNonMappingRows, getNumRows(connection, AbstractPortalMetricsUtility.SERVICE_METRICS_DETAILS));
        assertEquals(expectedNumPublishedServices, getNumRows(connection, AbstractPortalMetricsUtility.PUBLISHED_SERVICE));
        assertEquals(expectedNumMappingKeyRows, getNumRows(connection, AbstractPortalMetricsUtility.MAPPING_KEYS));
        assertEquals(expectedNumMappingValueRows, getNumRows(connection, AbstractPortalMetricsUtility.MAPPING_VALUES));
    }

    protected int getNumRows(final Connection connection, final String tableName) throws Exception {
        final ResultSet resultSet = connection.createStatement().executeQuery("SELECT COUNT(*) AS C FROM " + tableName);
        int numRows = 0;
        if (resultSet.next()) {
            numRows = resultSet.getInt("C");
        }
        return numRows;
    }

    protected List<ServiceMetric> getAllServiceMetrics(final Connection connection) throws Exception {
        final List<ServiceMetric> serviceMetrics = new ArrayList<ServiceMetric>();
        final ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM service_metrics");
        while (resultSet.next()) {
            final ServiceMetric metric = new ServiceMetric();
            metric.setId(resultSet.getLong("objectid"));
            metric.setNodeId(resultSet.getString("nodeid"));
            metric.setServiceId(resultSet.getLong("published_service_oid"));
            metric.setResolution(resultSet.getInt("resolution"));
            metric.setPeriodStart(resultSet.getLong("period_start"));
            metric.setIntervalSize(resultSet.getInt("interval_size"));
            metric.setServiceState(resultSet.getString("service_state"));
            metric.setStartTime(resultSet.getLong("start_time"));
            metric.setEndTime(resultSet.getLong("end_time"));
            metric.setAttempted(resultSet.getInt("attempted"));
            metric.setAuthorized(resultSet.getInt("authorized"));
            metric.setCompleted(resultSet.getInt("completed"));
            metric.setBackMin(getNullableInt(resultSet, "back_min"));
            metric.setBackMax(getNullableInt(resultSet, "back_max"));
            metric.setBackSum(resultSet.getInt("back_sum"));
            metric.setFrontMin(getNullableInt(resultSet, "front_min"));
            metric.setFrontMax(getNullableInt(resultSet, "front_max"));
            metric.setFrontSum(resultSet.getInt("front_sum"));
            metric.setUuid(resultSet.getString("uuid"));
            serviceMetrics.add(metric);
        }
        return serviceMetrics;
    }

    protected List<ServiceMetricDetail> getAllDetails(final Connection connection) throws Exception {
        final List<ServiceMetricDetail> details = new ArrayList<ServiceMetricDetail>();
        final ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM service_metrics_details");
        while (resultSet.next()) {
            final ServiceMetricDetail detail = new ServiceMetricDetail();
            detail.setServiceMetricsId(resultSet.getLong("service_metrics_oid"));
            detail.setMappingValuesId(resultSet.getLong("mapping_values_oid"));
            detail.setAttempted(resultSet.getInt("attempted"));
            detail.setAuthorized(resultSet.getInt("authorized"));
            detail.setCompleted(resultSet.getInt("completed"));
            detail.setBackMin(getNullableInt(resultSet, "back_min"));
            detail.setBackMax(getNullableInt(resultSet, "back_max"));
            detail.setBackSum(resultSet.getInt("back_sum"));
            detail.setFrontMin(getNullableInt(resultSet, "front_min"));
            detail.setFrontMax(getNullableInt(resultSet, "front_max"));
            detail.setFrontSum(resultSet.getInt("front_sum"));
            details.add(detail);
        }
        return details;
    }

    protected List<MappingValue> getAllMappingValues(final Connection connection) throws Exception {
        final List<MappingValue> values = new ArrayList<MappingValue>();
        final ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM message_context_mapping_values");
        while (resultSet.next()) {
            final MappingValue value = new MappingValue();
            value.setId(resultSet.getLong("objectid"));
            value.setDigested(resultSet.getString("digested"));
            value.setMappingKeyId(resultSet.getLong("MAPPING_KEYS_OID"));
            value.setProviderId(resultSet.getLong("AUTH_USER_PROVIDER_ID"));
            value.setAuthUserId(resultSet.getString("AUTH_USER_ID"));
            value.setAuthUserUniqueId(resultSet.getString("AUTH_USER_UNIQUE_ID"));
            value.setServiceOperation(resultSet.getString("SERVICE_OPERATION"));
            value.setMapping1Value(resultSet.getString("MAPPING1_VALUE"));
            value.setMapping2Value(resultSet.getString("MAPPING2_VALUE"));
            value.setMapping3Value(resultSet.getString("MAPPING3_VALUE"));
            value.setMapping4Value(resultSet.getString("MAPPING4_VALUE"));
            value.setMapping5Value(resultSet.getString("MAPPING5_VALUE"));
            value.setCreateTime(getNullableLong(resultSet, "CREATE_TIME"));
            values.add(value);
        }
        return values;
    }

    protected List<MappingKey> getAllMappingKeys(final Connection connection) throws Exception {
        final List<MappingKey> keys = new ArrayList<MappingKey>();
        final ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM message_context_mapping_keys");
        while (resultSet.next()) {
            final MappingKey key = new MappingKey();
            key.setId(resultSet.getLong("objectid"));
            key.setVersion(resultSet.getInt("VERSION"));
            key.setDigested(resultSet.getString("digested"));
            key.setMapping1Type(resultSet.getString("MAPPING1_TYPE"));
            key.setMapping1Key(resultSet.getString("MAPPING1_KEY"));
            key.setMapping2Type(resultSet.getString("MAPPING2_TYPE"));
            key.setMapping2Key(resultSet.getString("MAPPING2_KEY"));
            key.setMapping3Type(resultSet.getString("MAPPING3_TYPE"));
            key.setMapping3Key(resultSet.getString("MAPPING3_KEY"));
            key.setMapping4Type(resultSet.getString("MAPPING4_TYPE"));
            key.setMapping4Key(resultSet.getString("MAPPING4_KEY"));
            key.setMapping5Type(resultSet.getString("MAPPING5_TYPE"));
            key.setMapping5Key(resultSet.getString("MAPPING5_KEY"));
            key.setCreateTime(getNullableLong(resultSet, "CREATE_TIME"));
            keys.add(key);
        }
        return keys;
    }

    protected List<PublishedService> getAllPublishedServices(final Connection connection) throws Exception {
        final List<PublishedService> services = new ArrayList<PublishedService>();
        final ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM published_service");
        while (resultSet.next()) {
            final PublishedService service = new PublishedService();
            service.setId(resultSet.getLong("objectid"));
            service.setVersion(resultSet.getInt("version"));
            service.setName(resultSet.getString("NAME"));
            service.setPolicyXml(resultSet.getString("POLICY_XML"));
            service.setPolicyId(getNullableLong(resultSet, "POLICY_OID"));
            service.setWsdlUrl(resultSet.getString("WSDL_URL"));
            service.setWsdlXml(resultSet.getString("WSDL_XML"));
            service.setDisabled(resultSet.getInt("DISABLED"));
            service.setSoap(resultSet.getInt("SOAP"));
            service.setInternal(resultSet.getInt("INTERNAL"));
            service.setRoutingUri(resultSet.getString("ROUTING_URI"));
            service.setDefaultRoutingUrl(resultSet.getString("DEFAULT_ROUTING_URL"));
            service.setHttpMethods(resultSet.getString("HTTP_METHODS"));
            service.setLaxResolution(resultSet.getInt("LAX_RESOLUTION"));
            service.setWssProcessing(resultSet.getInt("WSS_PROCESSING"));
            service.setTracing(resultSet.getInt("TRACING"));
            service.setFolderId(getNullableLong(resultSet, "FOLDER_OID"));
            service.setSoapVersion(resultSet.getString("SOAP_VERSION"));
            service.setUuid(resultSet.getString("uuid"));
            services.add(service);
        }
        return services;
    }

    protected Long getNullableLong(final ResultSet resultSet, final String columnName) throws SQLException {
        Long result = null;
        final String valueAsString = resultSet.getString(columnName);
        if (valueAsString != null && !valueAsString.isEmpty()) {
            result = Long.valueOf(valueAsString);
        }
        return result;
    }

    protected Integer getNullableInt(final ResultSet resultSet, final String columnName) throws SQLException {
        Integer result = null;
        final String valueAsString = resultSet.getString(columnName);
        if (valueAsString != null && !valueAsString.isEmpty()) {
            result = Integer.valueOf(valueAsString);
        }
        return result;
    }

    /**
     * Required for queries that have 'IGNORE' keyword.
     */
    protected void replaceQueriesWithHsqlEquivalent() {
        // hsqldb does not support IGNORE
        AbstractPortalMetricsUtility.INSERT_PUBLISHED_SERVICE_QUERY = "MERGE INTO PUBLISHED_SERVICE USING (VALUES(" +
                "CAST(? AS BIGINT)," +
                "CAST(? AS INT)," +
                "CAST(? AS VARCHAR(225)),CAST(? AS VARCHAR(225))," +
                "CAST(? AS BIGINT)," +
                "CAST(? AS VARCHAR(255)),CAST(? AS VARCHAR(225))," +
                "CAST(? AS INT),CAST(?AS INT),CAST(? AS INT)," +
                "CAST(? AS VARCHAR(255)),CAST(? AS VARCHAR(255)),CAST(? AS VARCHAR(255))," +
                "CAST(? AS INT),CAST(? AS INT),CAST(? AS INT)," +
                "CAST(? AS BIGINT)," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(48)))) " +
                "AS VALS(a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s) ON PUBLISHED_SERVICE.OBJECTID = VALS.a " +
                "WHEN NOT MATCHED THEN INSERT VALUES VALS.a, VALS.b, VALS.c, VALS.d, VALS.e, VALS.f, VALS.g, VALS.h, VALS.i, VALS.j, VALS.k, VALS.l, VALS.m, VALS.n, VALS.o, VALS.p, VALS.q, VALS.r, VALS.S";
        AbstractPortalMetricsUtility.INSERT_MESSAGE_CONTEXT_MAPPING_KEYS_QUERY = "MERGE INTO MESSAGE_CONTEXT_MAPPING_KEYS USING (VALUES(" +
                "CAST(? AS BIGINT)," +
                "CAST(? AS INT)," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS BIGINT)))" +
                "AS VALS(a,b,c,d,e,f,g,h,i,j,k,l,m,n) ON MESSAGE_CONTEXT_MAPPING_KEYS.OBJECTID = VALS.a " +
                "WHEN NOT MATCHED THEN INSERT VALUES VALS.a, VALS.b, VALS.c, VALS.d, VALS.e, VALS.f, VALS.g, VALS.h, VALS.i, VALS.j, VALS.k, VALS.l, VALS.m, VALS.n";

        AbstractPortalMetricsUtility.INSERT_MESSAGE_CONTEXT_MAPPING_VALUES_QUERY = "MERGE INTO MESSAGE_CONTEXT_MAPPING_VALUES USING (VALUES(" +
                "CAST(? AS BIGINT)," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS BIGINT)," +
                "CAST(? AS BIGINT)," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS VARCHAR(255))," +
                "CAST(? AS BIGINT)))" +
                "AS VALS(a,b,c,d,e,f,g,h,i,j,k,l,m) ON MESSAGE_CONTEXT_MAPPING_VALUES.OBJECTID = VALS.a " +
                "WHEN NOT MATCHED THEN INSERT VALUES VALS.a, VALS.b, VALS.c, VALS.d, VALS.e, VALS.f, VALS.g, VALS.h, VALS.i, VALS.j, VALS.k, VALS.l, VALS.m";
    }
}
