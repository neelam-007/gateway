package com.l7tech.portal.metrics;

import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.MasterPasswordManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * Main class for com.l7tech.portal.metrics.PortalMetricsSynchUtility.
 * <p/>
 * Usage:<br />
 * test database connections - test properties_file_location<br />
 * sync metrics data - sync properties_file_location interval_in_minutes(optional)<br />
 * upgrade metrics data - upgrade properties_file_location<br />
 * encrypt a password - encrypt properties_file_location password_to_encrypt<br />
 * <p/>
 * Relevant tables: published_service, service_metrics, service_metrics_details, message_context_mapping_keys, message_context_mapping_values.
 *
 * @author alee
 */
public class PortalMetricsSyncUtilityMain {
    private static final Logger LOGGER = Logger.getLogger(PortalMetricsSyncUtilityMain.class);
    private static final String PORTAL_MANAGED_SERVICE_CLASSNAME = "com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService";

    // defaults
    private static final String DEFAULT_SOURCE_URL = "jdbc:mysql://localhost:3306/ssg";
    private static final String DEFAULT_SOURCE_USERNAME = "gateway";
    private static final String DEFAULT_SOURCE_PASSWORD = "7layer";
    private static final String DEFAULT_DEST_URL = "jdbc:mysql://localhost:3306/lrsdata";
    private static final String DEFAULT_DEST_USERNAME = "lrs";
    private static final String DEFAULT_OMP_FILE = "/opt/SecureSpan/Gateway/node/default/etc/conf/omp.dat";

    private static final String DEFAULT_DEST_PASSWORD = "lrs";
    // property keys
    private static final String SOURCE_URL = "source.url";
    private static final String SOURCE_USERNAME = "source.username";
    private static final String SOURCE_PASSWORD = "source.password";
    private static final String DEST_URL = "dest.url";
    private static final String DEST_USERNAME = "dest.username";
    private static final String DEST_PASSWORD = "dest.password";

    private static final String OMP_DAT_FILE_LOCATION = "omp.dat.file.location";
    private static final String BATCH_SIZE = "batch.size";

    public static void main(final String[] args) throws SQLException {
        if (args.length != 2 && args.length != 3) {
            printUsageAndExit();
        }
        final String action = args[0];
        final String propertiesFile = args[1];

        Properties properties = null;
        try {
            properties = loadProperties(propertiesFile);
        } catch (final IOException e) {
            LogUtil.logErrorAndExit(LOGGER, "Error loading properties from file.", e);
        }

        final File ompFile = new File(getProperty(properties, OMP_DAT_FILE_LOCATION, DEFAULT_OMP_FILE));
        final MasterPasswordManager.MasterPasswordFinder passwordFinder = new DefaultMasterPasswordFinder(ompFile);
        final MasterPasswordManager passwordManager = new MasterPasswordManager(passwordFinder);

        if ("encrypt".equalsIgnoreCase(action)) {
            LOGGER.debug("encrypt selected");
            if (args.length == 3) {
                System.out.println(passwordManager.encryptPassword(args[2].toCharArray()));
            } else {
                printUsageAndExit();
            }
        } else {
            final DatabaseInfo sourceInfo = createSourceDatabaseInfo(properties, passwordManager);
            final DatabaseInfo destInfo = createDestinationDatabaseInfo(properties, passwordManager);
            if ("test".equalsIgnoreCase(action)) {
                LOGGER.debug("test selected");
                testDatabaseConnections(sourceInfo, destInfo);
            } else if ("sync".equalsIgnoreCase(action)) {
                LOGGER.debug("SYNC selected.");
                logAndSync(args, properties, sourceInfo, destInfo);
            } else if ("upgrade".equalsIgnoreCase(action)) {
                LOGGER.debug("upgrade selected");
                new PortalMetricsUpgradeUtility(sourceInfo, destInfo).upgrade2_0To2_1();
            } else {
                printUsageAndExit();
            }
        }
    }

    private static void logAndSync(final String[] args, final Properties properties, final DatabaseInfo sourceDatabaseInfo, final DatabaseInfo destDatabaseInfo) {
        final Date start = new Date();
        LOGGER.info("Sync started at " + start);
        try {
            final Map<Long, String> portalManagedUUIDs = getPortalManagedUUIDs(sourceDatabaseInfo);
            if(portalManagedUUIDs.isEmpty())
                 return;
            final Integer batchSize = getBatchSize(properties);
            if (args.length == 3) {
                final Integer intervalInMinutes = Integer.valueOf(args[2]);
                LOGGER.debug("Metrics Bin Interval = "+intervalInMinutes);
                sync(sourceDatabaseInfo, destDatabaseInfo, portalManagedUUIDs, batchSize);
                aggregate(sourceDatabaseInfo, destDatabaseInfo, portalManagedUUIDs, intervalInMinutes);
            } else {
                sync(sourceDatabaseInfo, destDatabaseInfo, portalManagedUUIDs, batchSize);
            }
        } catch (final ClusterPropertyException e) {
            LogUtil.logError(LOGGER, "Could not retrieve portal managed services cluster property.", e);
        } catch (final Exception e) {
            LogUtil.logError(LOGGER, "Unexpected error.", e);
        } finally {
            final Date end = new Date();
            final long totalTime = end.getTime() - start.getTime();
            LOGGER.info("Total execution time: " + totalTime + " milliseconds.");
        }
    }

    private static Integer getBatchSize(Properties properties) {
        final String batchSizeString = getProperty(properties, BATCH_SIZE, null);
        Integer batchSize = null;
        if (batchSizeString != null && !batchSizeString.isEmpty()) {
            try {
                batchSize = Integer.valueOf(batchSizeString);
            } catch (final NumberFormatException e) {
                LogUtil.logError(LOGGER, "Invalid batch size: " + batchSizeString + ". Using default batch size.", e);
            }
        }
        return batchSize;
    }

    private static DatabaseInfo createSourceDatabaseInfo(final Properties properties, final MasterPasswordManager passwordManager) {
        final String sourceUrl = getProperty(properties, SOURCE_URL, DEFAULT_SOURCE_URL);
        final String sourceUsername = getProperty(properties, SOURCE_USERNAME, DEFAULT_SOURCE_USERNAME);
        final String sourcePassword = getProperty(properties, SOURCE_PASSWORD, DEFAULT_SOURCE_PASSWORD);
        final String decrypted = new String(passwordManager.decryptPasswordIfEncrypted(sourcePassword));
        return new DatabaseInfo(sourceUrl, sourceUsername, decrypted);
    }

    private static DatabaseInfo createDestinationDatabaseInfo(final Properties properties, final MasterPasswordManager passwordManager) {
        final String destUrl = getProperty(properties, DEST_URL, DEFAULT_DEST_URL);
        final String destUsername = getProperty(properties, DEST_USERNAME, DEFAULT_DEST_USERNAME);
        final String destPassword = getProperty(properties, DEST_PASSWORD, DEFAULT_DEST_PASSWORD);
        final String decrypted = new String(passwordManager.decryptPasswordIfEncrypted(destPassword));
        return new DatabaseInfo(destUrl, destUsername, decrypted);
    }

    private static void testDatabaseConnections(final DatabaseInfo sourceInfo, final DatabaseInfo destInfo) throws SQLException {
        LOGGER.info("Testing database connections.");
        Connection sourceConnection = null;
        Connection destConnection = null;
        try {
            sourceConnection = DriverManager.getConnection(sourceInfo.getUrl(), sourceInfo.getUsername(), sourceInfo.getPassword());
            destConnection = DriverManager.getConnection(destInfo.getUrl(), destInfo.getUsername(), destInfo.getPassword());
        } catch (final SQLException e) {
            closeConnections(sourceConnection, destConnection);
            throw e;
        }
        LOGGER.info("Database connections okay.");
    }

    private static void sync(final DatabaseInfo sourceDatabaseInfo, final DatabaseInfo destDatabaseInfo, final Map<Long,String> portalManagedServiceIds, final Integer batchSize) {
        final MySQLSyncUtility synchUtility = new MySQLSyncUtility(sourceDatabaseInfo, destDatabaseInfo);
        if (batchSize != null) {
            synchUtility.setBatchSize(batchSize);
        }
        try {
            synchUtility.syncHourlyData(portalManagedServiceIds);
            LOGGER.info("Finished syncing hourly data.");
        } catch (final Exception e) {
            LogUtil.logError(LOGGER, "Error syncing hourly data:" + e.getMessage(), e);
        }
    }

    private static void aggregate(final DatabaseInfo sourceDatabaseInfo, final DatabaseInfo destDatabaseInfo, final Map<Long, String> portalManagedServiceIds, final Integer interval) {
        if (interval != null) {
            final MySQLAggregateAndSyncUtility aggregateAndSyncUtility = new MySQLAggregateAndSyncUtility(sourceDatabaseInfo, destDatabaseInfo);
            try {
                aggregateAndSyncUtility.aggregateAndSync(portalManagedServiceIds, interval);
                LOGGER.info("Finished aggregating fine data.");
            } catch (final Exception e) {
                LogUtil.logError(LOGGER, "Error aggregating fine data: " + e.getMessage(), e);
            }
        } else {
            LOGGER.info("No interval specified. Skipped aggregation of fine data.");
        }
    }

    private static Map<Long,String> getPortalManagedUUIDs(final DatabaseInfo databaseInfo) {
        final DatabaseGenericEntityService  genericServiceService = new DatabaseGenericEntityService(databaseInfo, "generic_entity", "name", "description", "classname");
        final Map<Long,String> genericEntityValue = genericServiceService.getGenericEntityValue(PORTAL_MANAGED_SERVICE_CLASSNAME);

        if (genericEntityValue.isEmpty())
            LOGGER.info("No portal managed services");
        return genericEntityValue;
    }

    private static String getProperty(final Properties properties, final String propertyName, final String defaultValue) {
        String value = properties.getProperty(propertyName);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    private static Properties loadProperties(final String fileLocation) throws IOException {
        PropertyConfigurator.configure(fileLocation);
        final FileInputStream sourcePropertiesInputStream = new FileInputStream(fileLocation);
        final Properties properties = new Properties();
        properties.load(sourcePropertiesInputStream);
        return properties;
    }

    private static void printUsageAndExit() {
        System.err.println("com.l7tech.portal.metrics.PortalMetricsSyncUtilityMain usage:");
        System.err.println("test properties_file_location");
        System.err.println("sync properties_file_location interval_in_minutes");
        System.err.println("upgrade properties_file_location");
        System.err.println("encrypt properties_file_location password_to_encrypt");
        System.exit(0);
    }

    private static void closeConnection(final Connection connection) throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    private static void closeConnections(final Connection first, final Connection second) throws SQLException {
        try {
            closeConnection(first);
        } finally {
            closeConnection(second);
        }
    }
}
