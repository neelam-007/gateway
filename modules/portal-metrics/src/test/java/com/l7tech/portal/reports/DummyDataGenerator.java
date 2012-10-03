package com.l7tech.portal.reports;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.commons.lang.StringUtils;

import java.sql.*;
import java.util.*;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.commons.lang.StringUtils;

import java.sql.*;
import java.util.*;

/**
 * Populates a database with dummy metrics data.
 * <br />
 * Expected arguments: dbUrl user pass hour/15min numDaysDataToGenerate serviceId apiId maxAttempts maxErrors maxFrontSum maxBackSum [commaSeparatedKeys commaSeparatedMethods]/numKeysAndMethods
 * <br />
 * MaxAttempts, maxErrors, maxFrontSum, maxBackSum are used as upper limits for random number generation.
 * <p>
 * Examples:
 * <br />
 * 1. jdbc:mysql://foo.bar.com:3306/lrsdata lrs lrs hour 30 11111 5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4 200 50 3000 500 key1,key2,key3 method1,method2,method3
 * <br />
 * 2. jdbc:mysql://foo.bar.com:3306/lrsdata lrs lrs 15min 30 22222 5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4 200 50 3000 500 key1 method1
 * <br />
 * 3. jdbc:mysql://foo.bar.com:3306/lrsdata lrs lrs 15min 30 22222 5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4 200 50 3000 500 5
 * </p>
 */
public class DummyDataGenerator {
    private static final String SERVICE_METRICS = "service_metrics";
    private static final String SERVICE_METRICS_DETAILS = "service_metrics_details";
    private static final String MAPPING_KEYS = "message_context_mapping_keys";
    private static final String MAPPING_VALUES = "message_context_mapping_values";
    private static final String PUBLISHED_SERVICE = "published_service";
    static final String INSERT_SERVICE_METRICS_QUERY = "INSERT INTO " + SERVICE_METRICS +
            " (nodeid,published_service_oid,resolution,period_start,start_time,interval_size,end_time,service_state," +
            "attempted,authorized,completed,back_min,back_max,back_sum,front_min,front_max,front_sum,uuid) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static final String INSERT_SERVICE_METRICS_DETAILS_QUERY = "INSERT INTO " + SERVICE_METRICS_DETAILS +
            " (service_metrics_oid,mapping_values_oid,attempted,authorized,completed,back_min,back_max,back_sum," +
            "front_min,front_max,front_sum) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    static String INSERT_MESSAGE_CONTEXT_MAPPING_VALUES_QUERY = "INSERT IGNORE INTO " + MAPPING_VALUES + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static String INSERT_MESSAGE_CONTEXT_MAPPING_KEYS_QUERY = "INSERT IGNORE INTO " + MAPPING_KEYS + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    static String INSERT_PUBLISHED_SERVICE_QUERY = "INSERT IGNORE INTO " + PUBLISHED_SERVICE + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static void main(final String[] args) throws Exception {
        if (args.length != 12 && args.length != 13) {
            System.out.println("Usage: DummyDataGenerator dbUrl user pass hour/15min numDaysDataToGenerate serviceId apiId maxAttempts maxErrors maxFrontSum maxBackSum [commaSeparatedKeys commaSeparatedMethods]/numKeysAndMethods ");
            System.exit(0);
        }

        final String dbUrl = args[0];
        final String username = args[1];
        final String password = args[2];
        final String unit = args[3];
        final int numDays = Integer.valueOf(args[4]);
        final long serviceId = Long.valueOf(args[5]);
        final String uuid =  args[6];

        final int maxAttempts = Integer.valueOf(args[7]);
        final int maxErrors = Integer.valueOf(args[8]);
        final int maxFrontSum = Integer.valueOf(args[9]);
        final int maxBackSum = Integer.valueOf(args[10]);

        String[] splitKeys = null;
        String[] splitMethods = null;
        int numKeysAndMethods = 0;
        if (args.length == 13) {
            final String keys = args[11];
            splitKeys = StringUtils.split(keys, ",");
            final String methods = args[12];
            splitMethods = StringUtils.split(methods, ",");

            if (splitKeys.length != splitMethods.length) {
                System.out.println("The number of keys must match the number of methods.");
                System.exit(0);
            }
        } else {
            numKeysAndMethods = Integer.valueOf(args[11]);
        }

        final MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(dbUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        final Connection connection = dataSource.getConnection();

        final Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        final Random random = new Random();

        int numPerDay = 0;
        int calendarUnit = 0;
        int calendarUnitValue = 0;
        int resolution = 0;
        int intervalSize = 0;
        if (unit.equalsIgnoreCase("hour")) {
            numPerDay = 24;
            calendarUnit = Calendar.HOUR_OF_DAY;
            calendarUnitValue = -1;
            resolution = 1;
            intervalSize = 3600000;
        } else if (unit.equalsIgnoreCase("15min")) {
            numPerDay = 4 * 24;
            calendarUnit = Calendar.MINUTE;
            calendarUnitValue = -15;
            resolution = 3;
            intervalSize = 900000;
        } else {
            System.out.println("Unsupported unit: " + unit);
            System.exit(0);
        }

        insertPublishedService(connection, serviceId,uuid, "Test" + serviceId);

        final int mappingKeyId = 1;
        final Map<String, String> mappingKeys = new LinkedHashMap<String, String>();
        mappingKeys.put("API_KEY", "Custom Mapping");
        mappingKeys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, mappingKeyId, mappingKeys);

        if (splitKeys != null) {
            for (int i = 0; i < splitKeys.length; i++) {
                final String key = splitKeys[i];
                final String method = splitMethods[i];
                final long mappingValueId = (key + method).hashCode();
                insertMappingValue(connection, mappingValueId, mappingKeyId, Arrays.asList(key, method));
            }
        } else {
            for (int i = 0; i < numKeysAndMethods; i++) {
                final String key = "key" + i;
                final String method = "method" + i;
                final long mappingValueId = (key + method).hashCode();
                insertMappingValue(connection, mappingValueId, mappingKeyId, Arrays.asList(key, method));
            }
        }

        for (int j = 0; j < numDays; j++) {
            for (int i = 0; i < numPerDay; i++) {
                final int attempts = random.nextInt(maxAttempts);
                final int errors = random.nextInt(maxErrors);
                final int difference = attempts - errors;
                final int successes = difference < 0 ? 0 : difference;
                int frontSum = random.nextInt(maxFrontSum);
                final int backSum = random.nextInt(maxBackSum);
                if (backSum > frontSum) {
                    frontSum = maxFrontSum;
                }
                final long time = calendar.getTime().getTime();

                final long generatedId = insertServiceMetric(connection, serviceId, uuid, time, resolution, intervalSize, attempts, successes, successes, 1, 2, backSum, 1, 2, frontSum);
                if (splitKeys != null) {
                    for (int k = 0; k < splitKeys.length; k++) {
                        final String key = splitKeys[k];
                        final String method = splitMethods[k];
                        final long mappingValueId = (key + method).hashCode();
                        insertServiceMetricDetail(connection, generatedId, mappingValueId, attempts, successes, successes, 1, 2, backSum, 1, 2, frontSum);
                    }
                } else {
                    for (int k = 0; k < numKeysAndMethods; k++) {
                        final String key = "key" + k;
                        final String method = "method" + k;
                        final long mappingValueId = (key + method).hashCode();
                        insertServiceMetricDetail(connection, generatedId, mappingValueId, attempts, successes, successes, 1, 2, backSum, 1, 2, frontSum);
                    }
                }

                calendar.add(calendarUnit, calendarUnitValue);
                System.out.println("day=" + j + ", metric=" + i);
            }
        }

        System.out.println("Done processing.");

    }

    private static void insertMappingKey(final Connection connection, final long objectId, Map<String, String> keys) throws Exception {
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
        insertStatement.setObject(++paramCount, 1); // create time
        insertStatement.execute();
    }

    private static void insertMappingValue(final Connection connection, final long objectId, final long mappingKeysOid, List<String> mappingValues) throws Exception {
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

        insertStatement.setObject(++paramCount, 1); // create time
        insertStatement.execute();
    }

    private static void insertServiceMetricDetail(final Connection connection, final long serviceMetricsOid, final long mappingValuesOid, final Integer attempted, final Integer authorized, final Integer completed, final Integer backMin, final Integer backMax, final Integer backSum, final Integer frontMin, final Integer frontMax, final Integer frontSum) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(INSERT_SERVICE_METRICS_DETAILS_QUERY);
        insertStatement.setLong(1, serviceMetricsOid); // service metrics oid
        insertStatement.setLong(2, mappingValuesOid); // mapping values oid
        setBaseMetricFields(insertStatement, 3, attempted, authorized, completed, backMin, backMax, backSum, frontMin, frontMax, frontSum);
        insertStatement.execute();
    }

    private static long insertServiceMetric(final Connection connection, final long serviceId, final String uuid,final long periodStart, final int resolution, final int intervalSize, final Integer attempted, final Integer authorized, final Integer completed, final Integer backMin, final Integer backMax, final Integer backSum, final Integer frontMin, final Integer frontMax, final Integer frontSum) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(INSERT_SERVICE_METRICS_QUERY, Statement.RETURN_GENERATED_KEYS);
        insertStatement.setString(1, "nodeId"); // node i
        insertStatement.setLong(2, serviceId); // service id
        insertStatement.setInt(3, resolution); // resolution
        insertStatement.setLong(4, periodStart); // period start
        insertStatement.setLong(5, periodStart); // start time
        insertStatement.setInt(6, intervalSize); // interval size
        insertStatement.setLong(7, periodStart); // end time
        insertStatement.setString(8, "ENABLED"); // service state
        int index = setBaseMetricFields(insertStatement, 9, attempted, authorized, completed, backMin, backMax, backSum, frontMin, frontMax, frontSum);
        insertStatement.setString(++index, uuid);
        insertStatement.execute();
        final ResultSet generatedKey = insertStatement.getGeneratedKeys();
        generatedKey.next();
        return generatedKey.getLong(1);
    }

    private static long insertServiceMetric(final Connection connection, final long serviceId, final long periodStart, final int resolution, final int intervalSize, final Integer attempted, final Integer authorized, final Integer completed, final Integer backMin, final Integer backMax, final Integer backSum, final Integer frontMin, final Integer frontMax, final Integer frontSum) throws Exception {
        return insertServiceMetric(connection, serviceId, null,periodStart, resolution, intervalSize, attempted, authorized, completed, backMin, backMax, backSum, frontMin, frontMax, frontSum);
    }

    private static void insertPublishedService(final Connection connection, final long serviceId, final String uuid, final String name) throws Exception {
        final PreparedStatement insertStatement = connection.prepareStatement(INSERT_PUBLISHED_SERVICE_QUERY);
        int count = 0;
        insertStatement.setLong(++count, serviceId); // object id
        insertStatement.setInt(++count, 1); //version
        insertStatement.setString(++count, name); //name
        insertStatement.setString(++count, "xml"); //policy xml
        insertStatement.setInt(++count, 1); // policy oid - nullable
        insertStatement.setString(++count, "wsdl url"); // wsdl url
        insertStatement.setString(++count, "xml"); // wsdl xml
        insertStatement.setInt(++count, 0); // disabled
        insertStatement.setInt(++count, 1); // soap
        insertStatement.setInt(++count, 0); // internal
        insertStatement.setString(++count, "routing uri"); // routing uri
        insertStatement.setString(++count, "default routing url"); // default routing url
        insertStatement.setString(++count, "POST"); // http methods
        insertStatement.setInt(++count, 0); // lax resolution
        insertStatement.setInt(++count, 1); // wss processing
        insertStatement.setInt(++count, 0); // tracing
        insertStatement.setInt(++count, 1); // folder oid - nullable
        insertStatement.setString(++count, "soap version"); // soap version
        insertStatement.setString(++count, uuid); // uuid
        insertStatement.execute();

    }

    private static void insertPublishedService(final Connection connection, final long serviceId, final String name) throws Exception {
        insertPublishedService(connection, serviceId,null,name);

    }

    private static int setBaseMetricFields(final PreparedStatement insertStatement, final int startingIndex, final Integer attempted, final Integer authorized, final Integer completed, final Integer backMin, final Integer backMax, final Integer backSum, final Integer frontMin, final Integer frontMax, final Integer frontSum) throws SQLException {
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
