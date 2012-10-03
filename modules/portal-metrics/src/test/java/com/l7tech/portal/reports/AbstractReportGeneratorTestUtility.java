package com.l7tech.portal.reports;

import com.l7tech.util.IOUtils;
import org.hsqldb.jdbc.JDBCDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public abstract class AbstractReportGeneratorTestUtility extends AbstractXmlTestUtility {
    private static final String SERVICE_METRICS = "service_metrics";
    private static final String SERVICE_METRICS_DETAILS = "service_metrics_details";
    private static final String MAPPING_KEYS = "message_context_mapping_keys";
    private static final String MAPPING_VALUES = "message_context_mapping_values";
    private static final String PUBLISHED_SERVICE = "published_service";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";
    private static final String DB = "jdbc:hsqldb:mem:testdb";
    protected Connection connection;
    protected MetricsReportGenerator generator;
    protected Map<String, Object> defaultValues;
    protected JDBCDataSource dataSource;

    static final String INSERT_SERVICE_METRICS_QUERY = "INSERT INTO " + SERVICE_METRICS +
            " (nodeid,published_service_oid,resolution,period_start,start_time,interval_size,end_time,service_state," +
            "attempted,authorized,completed,back_min,back_max,back_sum,front_min,front_max,front_sum, uuid) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static final String INSERT_SERVICE_METRICS_DETAILS_QUERY = "INSERT INTO " + SERVICE_METRICS_DETAILS +
            " (service_metrics_oid,mapping_values_oid,attempted,authorized,completed,back_min,back_max,back_sum," +
            "front_min,front_max,front_sum) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    static String INSERT_MESSAGE_CONTEXT_MAPPING_VALUES_QUERY = "INSERT INTO " + MAPPING_VALUES + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static String INSERT_MESSAGE_CONTEXT_MAPPING_KEYS_QUERY = "INSERT INTO " + MAPPING_KEYS + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static String INSERT_PUBLISHED_SERVICE_QUERY = "INSERT INTO " + PUBLISHED_SERVICE + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


    public void setupAbstractReportGeneratorTest() throws Exception {
        setupAbstractXmlTest();
        connection = DriverManager.getConnection(DB, USERNAME, PASSWORD);
        createTables(connection);
        dataSource = new JDBCDataSource();
        dataSource.setDatabase(DB);
        dataSource.setUser(USERNAME);
        dataSource.setPassword(PASSWORD);
        generator = new MetricsReportGenerator(dataSource);
        defaultValues = new HashMap<String, Object>();
    }

    public void teardownAbstractReportGeneratorTest() throws Exception {
        dropTables(connection);
        connection.close();
    }

    private void createTables(final Connection connection)
            throws Exception {
        connection.createStatement().execute(readFile("portal_metrics_database.sql"));
        createViews(connection);
    }

    private String readFile(final String file) throws IOException {
        final InputStream resourceAsStream = AbstractReportGeneratorTestUtility.class.getClassLoader().getResourceAsStream(file);
        if (resourceAsStream == null) {
            throw new IOException("File does not exist: " + file);
        }
        final byte[] fileBytes = IOUtils.slurpStream(resourceAsStream);
        resourceAsStream.close();
        return new String(fileBytes);
    }

    private void createViews(final Connection connection) throws Exception {
        connection.createStatement().execute(readFile("portal_metrics_views.sql"));
    }

    private void createPublishedServiceTable(Connection connection)
            throws SQLException {
        connection.createStatement().execute(
                "CREATE TABLE " + PUBLISHED_SERVICE + " (\n"
                        + "  objectid bigint NOT NULL,\n"
                        + "  version int NOT NULL,\n"
                        + "  name varchar(255) NOT NULL,\n"
                        + "  policy_xml varchar(255),\n"
                        + "  policy_oid bigint default NULL,\n"
                        + "  wsdl_url varchar(255),\n"
                        + "  wsdl_xml varchar(255),\n"
                        + "  disabled TINYINT DEFAULT 0 NOT NULL,\n"
                        + "  soap TINYINT DEFAULT 1 NOT NULL,\n"
                        + "  internal TINYINT DEFAULT 0 NOT NULL,\n"
                        + "  routing_uri varchar(128),\n"
                        + "  default_routing_url varchar(4096),\n"
                        + "  http_methods varchar(255),\n"
                        + "  lax_resolution TINYINT DEFAULT 0 NOT NULL,\n"
                        + "  wss_processing TINYINT DEFAULT 1 NOT NULL,\n"
                        + "  tracing TINYINT DEFAULT 0 NOT NULL,\n"
                        + "  folder_oid bigint,\n"
                        + "  soap_version VARCHAR(20) DEFAULT 'UNKNOWN',\n"
                        + "  PRIMARY KEY (objectid)\n" + ");");
    }

    private void createServiceMetricsTable(Connection connection)
            throws SQLException {
        connection
                .createStatement()
                .execute(
                        "CREATE TABLE "
                                + SERVICE_METRICS
                                + " (\n"
                                + "  objectid bigint NOT NULL IDENTITY,\n"
                                + "  nodeid VARCHAR(32) NOT NULL,\n"
                                + "  published_service_oid BIGINT NOT NULL,\n"
                                + "  resolution INTEGER NOT NULL,\n"
                                + "  period_start BIGINT NOT NULL,\n"
                                + "  start_time BIGINT NOT NULL,\n"
                                + "  interval_size INTEGER NOT NULL,\n"
                                + "  end_time BIGINT NOT NULL,\n"
                                + "  attempted INTEGER NOT NULL,\n"
                                + "  authorized INTEGER NOT NULL,\n"
                                + "  completed INTEGER NOT NULL,\n"
                                + "  back_min INTEGER,\n"
                                + "  back_max INTEGER,\n"
                                + "  back_sum INTEGER NOT NULL,\n"
                                + "  front_min INTEGER,\n"
                                + "  front_max INTEGER,\n"
                                + "  front_sum INTEGER NOT NULL,\n"
                                + "  service_state VARCHAR(16),\n"
                                + "  UNIQUE (nodeid, published_service_oid, resolution, period_start)\n"
                                + ");");
    }

    private void createMappingKeysTable(Connection connection)
            throws SQLException {
        connection.createStatement().execute(
                "CREATE TABLE " + MAPPING_KEYS + " (\n"
                        + "  objectid bigint NOT NULL,\n"
                        + "  version int NOT NULL,\n"
                        + "  digested char(36) NOT NULL,\n"
                        + "  mapping1_type varchar(36),\n"
                        + "  mapping1_key varchar(128),\n"
                        + "  mapping2_type varchar(36),\n"
                        + "  mapping2_key varchar(128),\n"
                        + "  mapping3_type varchar(36),\n"
                        + "  mapping3_key varchar(128),\n"
                        + "  mapping4_type varchar(36),\n"
                        + "  mapping4_key varchar(128),\n"
                        + "  mapping5_type varchar(36),\n"
                        + "  mapping5_key varchar(128),\n"
                        + "  create_time bigint,\n"
                        + "  PRIMARY KEY (objectid),\n" + ");");
    }

    private void createMappingValuesTable(Connection connection)
            throws SQLException {
        connection.createStatement().execute(
                "CREATE TABLE " + MAPPING_VALUES + " (\n"
                        + "  objectid bigint NOT NULL,\n"
                        + "  digested char(36) NOT NULL,\n"
                        + "  mapping_keys_oid bigint NOT NULL,\n"
                        + "  auth_user_provider_id bigint,\n"
                        + "  auth_user_id varchar(255),\n"
                        + "  auth_user_unique_id varchar(255),\n"
                        + "  service_operation varchar(255),\n"
                        + "  mapping1_value varchar(255),\n"
                        + "  mapping2_value varchar(255),\n"
                        + "  mapping3_value varchar(255),\n"
                        + "  mapping4_value varchar(255),\n"
                        + "  mapping5_value varchar(255),\n"
                        + "  create_time bigint,\n"
                        + "  PRIMARY KEY  (objectid),\n"
                        + "  FOREIGN KEY (mapping_keys_oid) REFERENCES "
                        + MAPPING_KEYS + " (objectid)\n" + ");");
    }

    private void createServiceMetricDetailsTable(Connection connection)
            throws SQLException {
        connection
                .createStatement()
                .execute(
                        "CREATE TABLE "
                                + SERVICE_METRICS_DETAILS
                                + " (\n"
                                + "  service_metrics_oid BIGINT NOT NULL,\n"
                                + "  mapping_values_oid BIGINT NOT NULL,\n"
                                + "  attempted INTEGER NOT NULL,\n"
                                + "  authorized INTEGER NOT NULL,\n"
                                + "  completed INTEGER NOT NULL,\n"
                                + "  back_min INTEGER,\n"
                                + "  back_max INTEGER,\n"
                                + "  back_sum INTEGER NOT NULL,\n"
                                + "  front_min INTEGER,\n"
                                + "  front_max INTEGER,\n"
                                + "  front_sum INTEGER NOT NULL,\n"
                                + "  PRIMARY KEY (service_metrics_oid, mapping_values_oid),\n"
                                + "  CONSTRAINT fk_sm FOREIGN KEY (service_metrics_oid) REFERENCES "
                                + SERVICE_METRICS
                                + " (objectid) ON DELETE CASCADE,\n"
                                + "  CONSTRAINT fk_mv FOREIGN KEY (mapping_values_oid) REFERENCES "
                                + MAPPING_VALUES + " (objectid)\n" + ");");
    }

    private void dropTables(final Connection connection) throws Exception {
        connection.createStatement().execute("DROP VIEW api_usage_view");
        connection.createStatement().execute("DROP VIEW api_key_or_method_usage_view");
        connection.createStatement().execute(
                "DROP TABLE " + SERVICE_METRICS_DETAILS);
        connection.createStatement().execute("DROP TABLE " + MAPPING_VALUES);
        connection.createStatement().execute("DROP TABLE " + MAPPING_KEYS);
        connection.createStatement().execute("DROP TABLE " + SERVICE_METRICS);
        connection.createStatement().execute("DROP TABLE " + PUBLISHED_SERVICE);
    }

    protected void insertMappingKey(final Connection connection, final long objectId, Map<String, String> keys) throws Exception {
        insertMappingKey(connection, objectId, 1L, keys);
    }

    protected void insertMappingKey(final Connection connection, final long objectId, final Long createTime, Map<String, String> keys) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(INSERT_MESSAGE_CONTEXT_MAPPING_KEYS_QUERY);
        int paramCount = 0;
        insertStatement.setLong(++paramCount, objectId); //  object id
        insertStatement.setInt(++paramCount, 1); // version
        insertStatement.setString(++paramCount, "d"); // digested
        if (keys == null) {
            keys = Collections.emptyMap();
        }
        final int numKeys = keys.size();
        if (numKeys <= 5) {
            for (final Map.Entry<String, String> entry : keys.entrySet()) {
                insertStatement.setString(++paramCount, entry.getValue()); // mapping type
                insertStatement.setString(++paramCount, entry.getKey()); // mapping key
            }
            final int numNullKeys = 5 - numKeys;
            for (int i = 0; i < numNullKeys; i++) {
                insertStatement.setString(++paramCount, null); // mapping type
                insertStatement.setString(++paramCount, null); // mapping key
            }
        } else {
            throw new IllegalArgumentException("Maximum of 5 keys allowed.");
        }
        insertStatement.setObject(++paramCount, createTime); // create time
        insertStatement.execute();
    }

    protected void insertMappingValue(final Connection connection, final long objectId, final long mappingKeysOid, final List<String> mappingValues) throws Exception {
        insertMappingValue(connection, objectId, mappingKeysOid, 1L, mappingValues);

    }

    private void insertMappingValue(final Connection connection, final long objectId, final long mappingKeysOid, final Long createTime, List<String> mappingValues) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(INSERT_MESSAGE_CONTEXT_MAPPING_VALUES_QUERY);
        int paramCount = 0;
        insertStatement.setLong(++paramCount, objectId); // object id
        insertStatement.setString(++paramCount, "d"); // digested
        insertStatement.setLong(++paramCount, mappingKeysOid); // mapping keys oid
        insertStatement.setLong(++paramCount, 1); // auth user provider id
        insertStatement.setString(++paramCount, "test"); // auth user id
        insertStatement.setString(++paramCount, "unique"); // auth user unique id
        insertStatement.setString(++paramCount, "op"); // service operation

        if (mappingValues == null) {
            mappingValues = Collections.EMPTY_LIST;
        }

        final int numValues = mappingValues.size();
        if (numValues <= 5) {
            for (final String value : mappingValues) {
                insertStatement.setString(++paramCount, value); // mapping value
            }
            final int numNullValues = 5 - numValues;
            for (int i = 0; i < numNullValues; i++) {
                insertStatement.setString(++paramCount, null); // null mapping value
            }
        }

        insertStatement.setObject(++paramCount, createTime); // create time
        insertStatement.execute();
    }

    protected void insertServiceMetricDetail(final Connection connection, final long serviceMetricsOid, final long mappingValuesOid) throws Exception {
        insertServiceMetricDetail(connection, serviceMetricsOid, mappingValuesOid, 1, 1, 1, 1);
    }

    private void insertServiceMetricDetail(final Connection connection, final long serviceMetricsOid, final long mappingValuesOid, final Integer backMin, final Integer backMax, final Integer frontMin, final Integer frontMax) throws Exception {
        insertServiceMetricDetail(connection, serviceMetricsOid, mappingValuesOid, 1, 1, 1, backMin, backMax, 1, frontMin, frontMax, 1);
    }

    protected void insertServiceMetricDetail(final Connection connection, final long serviceMetricsOid, final long mappingValuesOid, final Integer attempted, final Integer authorized, final Integer completed, final Integer backMin, final Integer backMax, final Integer backSum, final Integer frontMin, final Integer frontMax, final Integer frontSum) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(INSERT_SERVICE_METRICS_DETAILS_QUERY);
        insertStatement.setLong(1, serviceMetricsOid); // service metrics oid
        insertStatement.setLong(2, mappingValuesOid); // mapping values oid
        setBaseMetricFields(insertStatement, 3, attempted, authorized, completed, backMin, backMax, backSum, frontMin, frontMax, frontSum);
        insertStatement.execute();
    }

    protected long insertServiceMetric(final Connection connection, final long serviceId, final long periodStart, final long endTime, String uuid) throws Exception {
        return insertServiceMetric(connection, serviceId, periodStart, endTime, 1, 1, 1, 1, uuid);
    }

    protected long insertServiceMetric(final Connection connection, final long serviceId, final long periodStart, final long endTime, String uuid, int resolution) throws Exception {
        return insertServiceMetric(connection, serviceId, "node", periodStart, periodStart, endTime, resolution, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, uuid);
    }

    private long insertServiceMetric(final Connection connection, final long serviceId, final long periodStart, final long endTime, final Integer backMin, final Integer backMax, final Integer frontMin, final Integer frontMax, final String uuid) throws Exception {
        return insertServiceMetric(connection, serviceId, "node", periodStart, periodStart, endTime, 1, 1, 1, 1, 1, backMin, backMax, 1, frontMin, frontMax, 1, uuid);
    }

    protected synchronized long insertServiceMetric(final Connection connection, final long serviceId, final String nodeId, final long periodStart, final long startTime, final long endTime, final int resolution, final int intervalSize, final Integer attempted, final Integer authorized, final Integer completed, final Integer backMin, final Integer backMax, final Integer backSum, final Integer frontMin, final Integer frontMax, final Integer frontSum, String uuid) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(INSERT_SERVICE_METRICS_QUERY, Statement.RETURN_GENERATED_KEYS);
        insertStatement.setString(1, nodeId); // node id
        insertStatement.setLong(2, serviceId); // service id
        insertStatement.setInt(3, resolution); // resolution
        insertStatement.setLong(4, periodStart); // period start
        insertStatement.setLong(5, startTime); // start time
        insertStatement.setInt(6, resolution==1?3600000:900000); // interval size
        insertStatement.setLong(7, endTime); // end time
        insertStatement.setString(8, "ENABLED"); // service state
        int index = setBaseMetricFields(insertStatement, 9, attempted, authorized, completed, backMin, backMax, backSum, frontMin, frontMax, frontSum);
        insertStatement.setString(++index,uuid);
        insertStatement.execute();
        final ResultSet generatedKey = insertStatement.getGeneratedKeys();
        generatedKey.next();
        return generatedKey.getLong(1);
    }

    protected void insertPublishedService(final Connection connection, final long serviceId, String uuid) throws Exception {
        insertPublishedService(connection, serviceId, 1L, 1L, false, uuid);
    }

    private void insertPublishedService(final Connection connection, final long serviceId, final boolean disabled, String uuid, int resolution) throws Exception {
        insertPublishedService(connection, serviceId, 1L, 1L, disabled, uuid);
    }

    private void insertPublishedService(final Connection connection, final long serviceId, final Long policyId, final Long folderId, final boolean disabled, final String uuid) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(INSERT_PUBLISHED_SERVICE_QUERY);
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
}
