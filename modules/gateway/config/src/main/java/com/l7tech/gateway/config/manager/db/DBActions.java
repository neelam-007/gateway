package com.l7tech.gateway.config.manager.db;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.db.LiquibaseDBManager;
import com.l7tech.util.*;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Various DB manipulation and checking methods used by the configuration wizard. Can probably be made more generic to
 * be useful to others as well.
 *
 * User: megery
 */
public class DBActions {
    private static final Logger logger = Logger.getLogger(DBActions.class.getName());
    private static final String EOL_CHAR = SyspropUtil.getProperty("line.separator");
    private static final String DEFAULT_DB_URL = "jdbc:mysql://{0}:{1}/{2}?autoReconnect=false&characterEncoding=UTF8&characterSetResults=UTF8&socketTimeout=600000&connectTimeout=10000";
    private static final String DB_VERSION_UNKNOWN = "Unknown";

    public static final String CONNECTION_SUCCESSFUL_MSG = "Connection to the database was a success";
    public static final String CONNECTION_UNSUCCESSFUL_MSG = "Connection to the database was unsuccessful - see warning/errors for details";

    private static final String ERROR_CODE_AUTH_FAILURE = "28000";
    private static final String ERROR_CODE_UNKNOWNDB = "42000";

    private static final String JDBC_DRIVER_NAME = "com.mysql.jdbc.Driver";
    private static final String ADMIN_DB_NAME = "mysql";

    private static final String SQL_CREATE_DB = "create database ";
    private static final String SQL_USE_DB = "use ";
    private static final String SQL_GRANT_ALL = "grant all on ";
    private static final String SQL_REVOKE_ALL = "revoke all privileges on ";

    private static final String SQL_DROP_DB = "drop database ";

    private static final String GENERIC_DBCONNECT_ERROR_MSG = "There was an error while attempting to connect to the database. Please try again";

    static {
        try {
            Class.forName(JDBC_DRIVER_NAME);
        } catch (ClassNotFoundException cnfe) {
            logger.info("MySQL JDBC driver not available.");
        }
    }

    private static final String DB_NAME_VAR = "<db_name>";

    //
    // CONSTRUCTOR
    //
    public DBActions() {
    }

    //
    // PUBLIC METHODS
    //

    /**
     * Creates a new mysql database with the ssg schema.
     *
     * @param config         The database configuration to create the database on.
     * @param hosts          The hosts to allow create grants.
     * @param dbScriptFolder The path to the liquibase scripts folder to create the database
     * @param overwriteDb    If true and there is an existing database it will be drop and recreated.
     * @return The DBActionsResult describing the result of attempting to create the database.
     * @throws IOException This is thrown if there was an error connecting to the database.
     */
    @NotNull
    public DBActionsResult createDb(@NotNull final DatabaseConfig config, @Nullable final Set<String> hosts, @NotNull final String dbScriptFolder, final boolean overwriteDb) throws IOException {
        Connection conn = null;
        try {
            conn = getConnection(config, true);

            if (testForExistingDb(conn, config.getName())) {
                if (overwriteDb) {
                    //we should overwrite the db
                    dropDatabase(conn, config.getName(), false);
                } else {
                    return new DBActionsResult(StatusType.ALREADY_EXISTS);
                }
            }
            //create a new database.
            createDatabaseWithGrants(conn, config, hosts);

            Connection dbConn = null;
            try {
                dbConn = getConnection(config, false);
                dbConn.setAutoCommit(false); //start transaction

                LiquibaseDBManager liquibaseDBManager = new LiquibaseDBManager(dbScriptFolder);
                liquibaseDBManager.ensureSchema(dbConn);

                dbConn.commit();
                dbConn.setAutoCommit(true); //end transaction
            } finally {
                ResourceUtils.closeQuietly(dbConn);
            }

            return new DBActionsResult(StatusType.SUCCESS);
        } catch (LiquibaseException e) {
            final String msg = ExceptionUtils.getMessage(e);
            logger.warning("Could not create database. An exception occurred: " + msg);
            return new DBActionsResult(determineErrorStatus(e), msg, e);
        } catch (SQLException e) {
            final String msg = ExceptionUtils.getMessage(e);
            logger.warning("Could not create database. An exception occurred: " + msg);
            return new DBActionsResult(determineErrorStatus(e.getSQLState()), msg, e);
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
    }


    /**
     * Get a regular or admin connection.
     *
     * WARNING: admin connections do not connect to the database named in the config!
     */
    @NotNull
    public Connection getConnection(@NotNull final DatabaseConfig databaseConfig, final boolean admin) throws SQLException {
        return getConnection(databaseConfig, admin, admin);
    }

    /**
     * Get a regular or admin connection.
     *
     * @param databaseConfig The database configuration to use
     * @param admin          True to connect using admin credentials
     * @param adminDb        True to connect to the administration DB
     */
    @NotNull
    public Connection getConnection(@NotNull final DatabaseConfig databaseConfig, final boolean admin, final boolean adminDb) throws SQLException {
        return getConnection(
                databaseConfig.getHost(),
                databaseConfig.getPort(),
                adminDb ? ADMIN_DB_NAME : databaseConfig.getName(),
                admin ? databaseConfig.getDatabaseAdminUsername() : databaseConfig.getNodeUsername(),
                admin ? databaseConfig.getDatabaseAdminPassword() : databaseConfig.getNodePassword());
    }

    /**
     * Get a connection using the given database info
     *
     * @param hostname The host name of the database
     * @param port     The port
     * @param dbName   The database name
     * @param username The user name
     * @param password The password
     * @return The connection to the database
     * @throws SQLException
     */
    @NotNull
    public Connection getConnection(@NotNull final String hostname, final int port, @NotNull final String dbName, @NotNull final String username, @NotNull final String password) throws SQLException {
        return DriverManager.getConnection(makeConnectionString(hostname, port, dbName), username, password);
    }

    /**
     * Drops an existing database
     *
     * @param databaseConfig The database config to drop the database on.
     * @param hosts          The set of hosts to revoke the privileges on
     * @param logInfo        True to log to info, false to log to warning
     * @param doRevoke       True to revoke permissions of the database, false to leave them
     * @return Returns true if the database was properly dropped. Returns false if there was an error dropping the
     * database.
     */
    public boolean dropDatabase(@NotNull final DatabaseConfig databaseConfig, @Nullable final Set<String> hosts, final boolean logInfo, final boolean doRevoke) {
        final String pUsername = databaseConfig.getDatabaseAdminUsername();
        final String pPassword = databaseConfig.getDatabaseAdminPassword();

        boolean allIsWell = false;
        Connection conn = null;
        Statement stmt = null;
        try {
            //TODO: why does this create a copy adminConfig?
            DatabaseConfig adminConfig = new DatabaseConfig(databaseConfig);
            adminConfig.setDatabaseAdminUsername(pUsername);
            adminConfig.setDatabaseAdminPassword(pPassword);

            conn = getConnection(adminConfig, true);
            try {
                dropDatabase(conn, databaseConfig.getName(), logInfo);
                allIsWell = true;
            } catch (SQLException e) {
                logger.severe("Failure while dropping the database: " + ExceptionUtils.getMessage(e));
                allIsWell = false;
            }

            if (allIsWell && doRevoke) {
                stmt = conn.createStatement();
                String[] revokeStatements = getRevokeStatements(adminConfig, hosts);
                for (String revokeStatement : revokeStatements) {
                    try {
                        stmt.executeUpdate(revokeStatement);
                    } catch (SQLException e) {
                        logger.info("While revoking a permission for user " + databaseConfig.getNodeUsername() + " an error occurred. The privilege was not found.");
                        logger.info("This is not a cause for concern. The database is likely an older one.");
                        //don't want to halt on errors here
                    }
                }
            }
        } catch (SQLException connectException) {
            logger.severe("Failure while dropping the database. Could not connect to the admin database on the server (" + ADMIN_DB_NAME + ") as user " + pUsername + "\". Please drop the database manually.");
            allIsWell = false;
        } finally {
            ResourceUtils.closeQuietly(stmt);
            ResourceUtils.closeQuietly(conn);
        }
        return allIsWell;
    }


    /**
     * Drops an existing database.
     *
     * @param connection The connection to drop the database on
     * @param dbName     The name for the database to drop
     * @param logInfo    True to log to info, false to log to warning
     * @throws SQLException
     */
    private void dropDatabase(@NotNull final Connection connection, @NotNull final String dbName, final boolean logInfo) throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(SQL_DROP_DB + dbName);

            if (logInfo)
                logger.info("dropping database \"" + dbName + "\"");
            else
                logger.warning("dropping database \"" + dbName + "\"");
        } finally {
            ResourceUtils.closeQuietly(statement);
        }
    }

    /**
     * Determine the current database version.
     *
     * @param databaseConfig The configuration for the database (admin creds optional)
     * @return The database version string or null on error.
     */
    @Nullable
    public String checkDbVersion(@NotNull final DatabaseConfig databaseConfig) {
        Connection conn = null;
        String dbVersion = null;
        try {
            conn = getConnection(databaseConfig, false);
            dbVersion = getDbVersion(conn);
        } catch (SQLException sqle) {
            if (sqle.getSQLState().equals(ERROR_CODE_AUTH_FAILURE) && databaseConfig.getDatabaseAdminUsername() != null) {
                // try again with admin credentials
                ResourceUtils.closeQuietly(conn);
                conn = null;

                try {
                    conn = getConnection(databaseConfig, true, false);
                    dbVersion = getDbVersion(conn);
                } catch (SQLException sqle2) {
                    dbVersion = DB_VERSION_UNKNOWN;
                    logger.warning("Error while checking the database version: " + ExceptionUtils.getMessage(sqle2));
                }
            } else {
                dbVersion = DB_VERSION_UNKNOWN;
                logger.warning("Error while checking the database version: " + ExceptionUtils.getMessage(sqle));
            }
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
        return dbVersion;
    }

    /**
     * Upgrade a database to the current version.
     *
     * @param config                        The configuration for the database (admin creds required)
     * @param databaseSpecificScriptsFolder The folder holding the mysql upgrade scripts.
     * @param liquibaseUpgradeFolder        The folder holding the liquibase scripts
     * @param currentVersion                The software version. only used to check if the db is already at the current
     *                                      version.
     * @param ui                            The listener for any feedback
     * @return true if the update was successful, false otherwise.
     * @throws IOException If an error occurs
     */
    public boolean upgradeDb(@NotNull final DatabaseConfig config,
                             @NotNull final String databaseSpecificScriptsFolder,
                             @NotNull final String liquibaseUpgradeFolder,
                             @NotNull final String currentVersion,
                             @Nullable final DBActionsListener ui) throws IOException {
        boolean success = false;

        final DatabaseConfig databaseConfig = new DatabaseConfig(config);
        final String hostname = databaseConfig.getHost();
        final String dbName = databaseConfig.getName();
        final String username = databaseConfig.getNodeUsername();
        final String password = databaseConfig.getNodePassword();

        logger.info("Attempting to connect to an existing database (" + hostname + "/" + dbName + ")" + " using username/password " + username + "/" + hidepass(password));

        final DBActions.DBActionsResult status = checkExistingDb(databaseConfig);
        if (status.getStatus() == StatusType.SUCCESS) {
            logger.info(CONNECTION_SUCCESSFUL_MSG);

            logger.info("Now Checking database version.");
            final String dbVersion = checkDbVersion(databaseConfig);
            if (dbVersion == null || DB_VERSION_UNKNOWN.equals(dbVersion)) {
                String errorMsg = "The " + dbName + " database does not appear to be a valid Gateway database.";
                logger.warning(errorMsg);
                if (ui != null) ui.showErrorMessage(errorMsg);
            } else {
                if (dbVersion.equals(currentVersion)) {
                    logger.info("Database version is correct (" + dbVersion + ")");
                    if (ui != null) ui.hideErrorMessage();
                    success = true;
                } else {
                    try {
                        success = doDbUpgrade(config, databaseSpecificScriptsFolder, liquibaseUpgradeFolder, ui);
                        if (success && ui != null) ui.showSuccess("The database was successfully upgraded\n");
                    } catch (IOException e) {
                        final String errorMsg = "There was an error while attempting to upgrade the database";
                        logger.severe(errorMsg);
                        logger.severe(ExceptionUtils.getMessage(e));
                        if (ui != null) {
                            ui.showErrorMessage(errorMsg);
                        }
                    }
                }
            }
        } else {
            switch (status.getStatus()) {
                case UNKNOWNHOST_FAILURE:
                    String errorMsg = "Could not connect to the host: \"" + hostname + "\". Please check the hostname and try again.";
                    logger.info("Connection to the database for creating was unsuccessful - see warning/errors for details");
                    logger.warning(errorMsg);
                    if (ui != null) ui.showErrorMessage(errorMsg);
                    break;
                case AUTHORIZATION_FAILURE:
                    logger.info(CONNECTION_UNSUCCESSFUL_MSG);
                    errorMsg = MessageFormat.format("There was a connection error when attempting to connect to the database \"{0}\" using the username \"{1}\". " +
                                    "Perhaps the password is wrong. Either the username and/or password is incorrect, or the database \"{2}\" does not exist.",
                            dbName, username, dbName);
                    if (ui != null) ui.showErrorMessage(errorMsg);
                    logger.warning("There was an authentication error when attempting to connect to the database \"" + dbName + "\" using the username \"" +
                            username + "\" and password \"" + password + "\".");
                    break;
                case UNKNOWNDB_FAILURE:
                    logger.info(CONNECTION_UNSUCCESSFUL_MSG);
                    errorMsg = "Could not connect to the database \"" + dbName + "\". The database does not exist or the user \"" + username + "\" does not have permission to access it." +
                            "Please check your input and try again.";
                    if (ui != null) ui.showErrorMessage(errorMsg);
                    logger.warning("Could not connect to the database \"" + dbName + "\". The database does not exist.");
                    break;
                default:
                    logger.info(CONNECTION_UNSUCCESSFUL_MSG);
                    errorMsg = GENERIC_DBCONNECT_ERROR_MSG;
                    if (ui != null) ui.showErrorMessage(errorMsg);
                    logger.warning("There was an unknown error while attempting to connect to the database.");
                    break;
            }
        }

        return success;
    }

    public String getNodeIdForMac(final DatabaseConfig databaseConfig,
                                  final Collection<String> macAddresses) throws SQLException {
        String nodeid = null;

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection(databaseConfig, false);
            statement = conn.prepareStatement("SELECT nodeid FROM cluster_info WHERE mac IN (?,?,?,?,?,?,?,?,?,?)");
            Iterator<String> macIterator = macAddresses.iterator();
            for (int i = 0; i < 10; i++) {
                String mac = "-";
                if (macIterator.hasNext()) {
                    mac = macIterator.next();
                }
                statement.setString(i + 1, mac);
            }
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                nodeid = resultSet.getString(1);
            }
        } finally {
            ResourceUtils.closeQuietly(resultSet);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(conn);
        }

        return nodeid;
    }

    /**
     * Checks if the database exists and can be connected to.
     *
     * @param databaseConfig The database config to check
     * @return The result of the check
     */
    @NotNull
    public DBActionsResult checkExistingDb(@NotNull final DatabaseConfig databaseConfig) {

        Connection conn = null;

        try {
            conn = getConnection(databaseConfig, false);
            return new DBActionsResult(StatusType.SUCCESS);
        } catch (SQLException e) {
            String message = ExceptionUtils.getMessage(e);
            logger.warning("Could not login to the database using " + databaseConfig.getNodeUsername() + ":" + hidepass(databaseConfig.getNodePassword()) + "@" + InetAddressUtil.getHostForUrl(databaseConfig.getHost()) + ":" + databaseConfig.getPort() + "/" + databaseConfig.getName());
            logger.warning(message);
            return new DBActionsResult(determineErrorStatus(e.getSQLState()), message, e);
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
    }

    public void copyDatabase(@NotNull final DatabaseConfig sourceConfig, @NotNull final DatabaseConfig targetConfig, final boolean skipAudits, @Nullable final DBActionsListener ui) throws SQLException {
        Connection sourceConnection = null;
        Connection targetConnection = null;
        TimerTask spammer = null;
        try {
            sourceConnection = getConnection(sourceConfig, false);
            sourceConnection.setAutoCommit(false);
            if (ui != null) {
                spammer = new ProgressTimerTask(ui);
                Background.scheduleRepeated(spammer, 2000, 2000);
            }

            //create the test database
            createDatabase(targetConfig);

            targetConnection = getConnection(targetConfig, true, false);
            targetConnection.setAutoCommit(false);

            //copy the database
            copyDbSchema(sourceConnection, targetConnection);
            copyFunctions(sourceConnection, targetConnection);
            copyDatabaseContents(sourceConnection, targetConnection, skipAudits);

            targetConnection.commit();
            if (spammer != null) {
                Background.cancel(spammer);
                spammer = null;
            }
        } finally {
            ResourceUtils.closeQuietly(sourceConnection);
            ResourceUtils.closeQuietly(targetConnection);
            if (spammer != null) {
                Background.cancel(spammer);
            }
        }
    }

//
// PRIVATE METHODS
//

    //will return the version of the DB, or null if it cannot be determined
    @Nullable
    private String getDbVersion(@NotNull final Connection conn) {
        //this is an SSG database, now determine the exact version
        return DbUpgradeUtil.checkVersionFromDatabaseVersion(conn);
    }

    @NotNull
    private Set<String> getFunctionNames(@NotNull final Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            Set<String> functions = new LinkedHashSet<>();

            DatabaseMetaData metadata = conn.getMetaData();

            ResultSet functionNames = metadata.getFunctions(null, "%", "%");
            while (functionNames.next()) {
                String tableName = functionNames.getString("FUNCTION_NAME");
                functions.add(tableName);
            }
            return functions;
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
    }

    @NotNull
    private Set<String> getTableNames(@NotNull final Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            Set<String> tables = new LinkedHashSet<>();

            DatabaseMetaData metadata = conn.getMetaData();
            String[] tableTypes = {
                    "TABLE",
            };

            final ResultSet tableNames = metadata.getTables(null, "%", "%", tableTypes);
            while (tableNames.next()) {
                final String tableName = tableNames.getString("TABLE_NAME");
                tables.add(tableName);
            }
            return tables;
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
    }

    /**
     * Create DB and do grants, DB will be empty
     */
    public void createDatabaseWithGrants(@NotNull final Connection connection, @NotNull final DatabaseConfig databaseConfig, @Nullable final Set<String> hosts) throws SQLException, IOException {
        if (databaseConfig.getNodePassword().isEmpty()) {
            throw new CausedIOException("Cannot create database with empty password for user '" + databaseConfig.getNodeUsername() + "'.");
        }

        connection.setAutoCommit(false); //start transaction
        final String newDbName = databaseConfig.getName();

        logger.info("creating database \"" + newDbName + "\"");

        createDatabase(connection, newDbName);

        final String[] grantSql = getGrantStatements(databaseConfig, hosts);
        logger.info("Creating user \"" + databaseConfig.getNodeUsername() + "\" and performing grants on " + newDbName + " database");
        executeUpdates(connection, grantSql);

        connection.commit();     //finish transaction
        connection.setAutoCommit(true);
    }

    private void createDatabase(@NotNull final DatabaseConfig databaseConfig) throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection(databaseConfig, true);
            createDatabase(connection, databaseConfig.getName());
        } finally {
            ResourceUtils.closeQuietly(connection);
        }
    }

    private void createDatabase(@NotNull final Connection dbConnection, @NotNull final String dbname) throws SQLException {
        Statement stmt = null;
        try {
            stmt = dbConnection.createStatement();
            stmt.executeUpdate(SQL_CREATE_DB + dbname);
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
    }

    @NotNull
    private String[] getGrantStatements(@NotNull final DatabaseConfig databaseConfig, @Nullable final Set<String> hosts) {
        return getPermissionChangeStatements(databaseConfig, hosts, true);
    }

    @NotNull
    private String[] getRevokeStatements(@NotNull final DatabaseConfig databaseConfig, @Nullable final Set<String> hosts) {
        return getPermissionChangeStatements(databaseConfig, hosts, false);
    }

    private void executeUpdates(@NotNull final Connection connection, @NotNull final String[] sqlStatements) throws SQLException {
        Statement statement = null;

        if (sqlStatements.length > 0) {
            try {
                statement = connection.createStatement();

                for (String statementSql : sqlStatements) {
                    statement.executeUpdate(statementSql);
                }
            } finally {
                ResourceUtils.closeQuietly(statement);
            }
        }
    }

    @NotNull
    private String[] getPermissionChangeStatements(@NotNull final DatabaseConfig databaseConfig, @Nullable final Set<String> hosts, final boolean doGrants) {
        return new DBPermission(databaseConfig, hosts, doGrants).getStatements();
    }

    @NotNull
    private String[] getDbCreateFunctionStatementsFromDb(@NotNull final Connection dbConn) throws SQLException {

        Statement getCreateFunctionsStmt = null;
        final List<String> list = new ArrayList<>();
        try {
            getCreateFunctionsStmt = dbConn.createStatement();

            Set<String> functionNames = getFunctionNames(dbConn);

            for (String functionName : functionNames) {
                ResultSet createFunctions = getCreateFunctionsStmt.executeQuery("show create function " + functionName);
                while (createFunctions.next()) {
                    String functionString = createFunctions.getString(3);
                    if (functionString != null && !functionString.isEmpty()) {
                        String s = functionString.replace("\n", " ");
                        //need to replace the definer section otherwise the bd user that is creating these functions wont be able to call them.
                        s = s.replaceAll("CREATE DEFINER=`[^`]*`@`[^`]*` FUNCTION", "CREATE FUNCTION");
                        list.add(s);
                    }
                }
            }
        } finally {
            ResourceUtils.closeQuietly(getCreateFunctionsStmt);
        }

        return list.toArray(new String[list.size()]);
    }

    @NotNull
    private String[] getDbCreateStatementsFromDb(@NotNull final Connection dbConn) throws SQLException {

        Statement getCreateTablesStmt = null;
        List<String> list = new ArrayList<>();
        try {
            getCreateTablesStmt = dbConn.createStatement();

            final Set<String> tableNames = getTableNames(dbConn);

            //turn off foreign key checks so that tables with constraints can be created before the constraints exists
            list.add("SET FOREIGN_KEY_CHECKS = 0");
            for (final String tableName : tableNames) {
                ResultSet createTables = getCreateTablesStmt.executeQuery("show create table " + tableName);
                while (createTables.next()) {
                    String s = createTables.getString(2).replace("\n", "");
                    // The below replacement is needed because mysql will return a default byte value as the actual byte string.
                    // In the create table statement we need it to be a hex string.
                    s = s.replace("DEFAULT '\\0\\0\\0\\0\\0\\0\\0\\0��������'", "DEFAULT X'0000000000000000FFFFFFFFFFFFFFFF'");
                    s = s.replace("binary(16) NOT NULL DEFAULT '0\\0\\0\\0\\0\\0\\0\\0\\0\\0\\0\\0\\0\\0\\0\\0'", "binary(16) NOT NULL DEFAULT 0");
                    list.add(s);
                }
            }
        } finally {
            ResourceUtils.closeQuietly(getCreateTablesStmt);
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Tests if there is a database with the given name.
     *
     * @param connection The connection to look for the database on.
     * @param dbName     The database name to test for
     * @return true if there is a database with the given name, false otherwise.
     */
    private boolean testForExistingDb(final Connection connection, final String dbName) {
        boolean dbExists = false;

        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(SQL_USE_DB + dbName);
            dbExists = true;       //if an exception was not thrown then the db must already exist.
        } catch (SQLException e) {
            //this means the db didn't exist.
            dbExists = false;
        } finally {
            ResourceUtils.closeQuietly(statement);
        }

        return dbExists;
    }

    private String makeConnectionString(@NotNull final String hostname, final int port, @NotNull final String dbName) {
        String urlPattern = ConfigFactory.getProperty("com.l7tech.config.dburl", DEFAULT_DB_URL);
        if (urlPattern != null) {
            return MessageFormat.format(urlPattern, InetAddressUtil.getHostForUrl(hostname), Integer.toString(port), dbName);
        } else {
            throw new RuntimeException("The database connection url pattern is null. You must give a value to the 'com.l7tech.config.dburl' property.");
        }
    }

    private boolean doDbUpgrade(@NotNull final DatabaseConfig databaseConfig, @NotNull final String databaseSpecificScriptsFolder, @NotNull final String liquibaseUpgradeFolder, @Nullable final DBActionsListener ui) throws IOException {
        boolean isOk;

        if (ui != null) ui.showSuccess("Testing the upgrade on a test database ... " + EOL_CHAR);

        final DBActions.DBActionsResult testUpgradeResult = testDbUpgrade(databaseConfig, databaseSpecificScriptsFolder, liquibaseUpgradeFolder, ui);
        if (testUpgradeResult.getStatus() != StatusType.SUCCESS) {
            String msg = "The database was not upgraded due to the following reasons:\n\n" + testUpgradeResult.getErrorMessage() + "\n\n" +
                    "No changes have been made to the database. Please correct the problem and try again.";
            if (ui != null) ui.showErrorMessage(msg);
            logger.warning(msg);
            isOk = false;
        } else {
            logger.info("Attempting to upgrade the existing database \"" + databaseConfig.getName() + "\"");
            DBActionsResult upgradeResult = upgradeDbSchema(databaseConfig, false, databaseSpecificScriptsFolder, liquibaseUpgradeFolder, ui);
            String msg;
            switch (upgradeResult.getStatus()) {
                case SUCCESS:
                    logger.info("The database was successfully upgraded");
                    isOk = true;
                    break;
                case AUTHORIZATION_FAILURE:
                    msg = "login to the database with the supplied credentials failed, please try again";
                    logger.warning(msg);
                    if (ui != null) ui.showErrorMessage(msg);
                    isOk = false;
                    break;
                case UNKNOWNHOST_FAILURE:
                    msg = "Could not connect to the host: \"" + databaseConfig.getHost() + "\". Please check the hostname and try again.";
                    logger.warning(msg);
                    if (ui != null) ui.showErrorMessage(msg);
                    isOk = false;
                    break;
                default:
                    msg = "Database upgrade process failed: " + upgradeResult.getErrorMessage();
                    if (ui != null) ui.showErrorMessage(msg);
                    logger.warning(msg);
                    isOk = false;
                    break;
            }
        }
        return isOk;
    }

    @NotNull
    private DBActionsResult testDbUpgrade(@NotNull final DatabaseConfig databaseConfig, @NotNull final String databaseSpecificScriptsFolder, @NotNull final String liquibaseUpgradeFolder, @Nullable final DBActionsListener ui) throws IOException {
        final DatabaseConfig testDatabaseConfig = new DatabaseConfig(databaseConfig);
        testDatabaseConfig.setName(databaseConfig.getName() + "_testUpgrade");
        try {
            if (ui != null)
                ui.showSuccess("Creating test database \"" + testDatabaseConfig.getName() + "\" (without audits)." + EOL_CHAR);
            if (ui != null) ui.showSuccess("Database creation may take a few minutes." + EOL_CHAR);

            copyDatabase(databaseConfig, testDatabaseConfig, true, ui);
            if (ui != null) ui.showSuccess("The test database was created." + EOL_CHAR);
            if (ui != null) ui.showSuccess("Upgrading the test database. This may take a few minutes." + EOL_CHAR);
            final DBActionsResult upgradeResult = upgradeDbSchema(testDatabaseConfig, true, databaseSpecificScriptsFolder, liquibaseUpgradeFolder, ui);
            if (upgradeResult.getStatus() != StatusType.SUCCESS) {
                return new DBActionsResult(StatusType.CANNOT_UPGRADE, upgradeResult.getErrorMessage(), null);
            } else {
                if (ui != null)
                    ui.showSuccess("The test database \"" + testDatabaseConfig.getName() + "\" was upgraded successfully." + EOL_CHAR);
                logger.info(testDatabaseConfig.getName() + " was upgraded successfully.");
                return new DBActionsResult(StatusType.SUCCESS);
            }
        } catch (SQLException e) {
            return new DBActionsResult(determineErrorStatus(e.getSQLState()), ExceptionUtils.getMessage(e), e);
        } finally {
            //get rid of the temp database
            dropDatabase(testDatabaseConfig, null, true, false);
        }
    }

    /**
     * Upgrades the databaseConfig to the latest version
     *
     * @param databaseConfig                The database to update
     * @param useAdminConnection            Should the admin connection be used, if false the Gateway user is used.
     * @param databaseSpecificScriptsFolder The mysql upgrade scripts folder
     * @param liquibaseUpgradeFolder        The liquibase sripts folder.
     * @param ui                            The ui to report status a messages to.
     * @return The status of the upgrade
     * @throws IOException
     */
    @NotNull
    private DBActionsResult upgradeDbSchema(@NotNull final DatabaseConfig databaseConfig,
                                            final boolean useAdminConnection,
                                            @NotNull final String databaseSpecificScriptsFolder,
                                            @NotNull final String liquibaseUpgradeFolder,
                                            @Nullable final DBActionsListener ui) throws IOException {
        //check that the liquibase scripts folder exists
        final File liquibaseScriptsFolder = new File(liquibaseUpgradeFolder);
        if (liquibaseScriptsFolder.isFile()) {
            throw new FileNotFoundException("File not found '" + liquibaseUpgradeFolder + "'.");
        }

        //the timer used to show upgrade progress
        TimerTask spammer = null;
        if (ui != null) {
            spammer = new ProgressTimerTask(ui);
            Background.scheduleRepeated(spammer, 2000, 2000);
        }
        Connection conn = null;
        try {
            conn = getConnection(databaseConfig, useAdminConnection, false);
            conn.setAutoCommit(false);

            final LiquibaseDBManager liquibaseDBManager = new LiquibaseDBManager(liquibaseUpgradeFolder);
            //check if this database has already had liquibase applied to it.
            if (!liquibaseDBManager.isLiquibaseDB(conn)) {
                //check that the mysql upgrade scripts folder exists
                final File dbScriptsFolder = new File(databaseSpecificScriptsFolder);
                if (dbScriptsFolder.isFile()) {
                    throw new FileNotFoundException("File not found '" + databaseSpecificScriptsFolder + "'.");
                }

                // need to upgrade the database to 8.3.pre so that liquibase changesets can be applied.
                final DBActionsResult result = legacyUpgrade(databaseConfig, conn, dbScriptsFolder, ui);
                //if it was unsuccessful return the error
                if (!StatusType.SUCCESS.equals(result.getStatus())) {
                    return result;
                }
                if (ui != null) {
                    ui.showSuccess("Upgrading \"" + databaseConfig.getName() + "\" from " + LiquibaseDBManager.JAVELIN_PRE_DB_VERSION + "->" + BuildInfo.getProductVersion() + EOL_CHAR);
                    ui.showSuccess("Please note that if you have a lot of audits, this may take a long time." + EOL_CHAR);
                }
                //perform liquibase upgrade
                liquibaseDBManager.updatePreliquibaseDB(conn);
            } else {
                if (ui != null) {
                    ui.showSuccess("Upgrading \"" + databaseConfig.getName() + "\" from " + getDbVersion(conn) + "->" + BuildInfo.getProductVersion() + EOL_CHAR);
                    ui.showSuccess("Please note that if you have a lot of audits, this may take a long time." + EOL_CHAR);
                }
                //update the database. This will apply any changesets that have not already been applied.
                liquibaseDBManager.ensureSchema(conn);
            }
            conn.commit();
            conn.setAutoCommit(true);
            return new DBActionsResult(StatusType.SUCCESS);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error during upgrade.", e);
            return new DBActionsResult(determineErrorStatus(e.getSQLState()), ExceptionUtils.getMessage(e), e);
        } catch (LiquibaseException e) {
            logger.log(Level.WARNING, "Error during upgrade.", e);
            return new DBActionsResult(determineErrorStatus(e), ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(conn);
            if (spammer != null) {
                Background.cancel(spammer);
            }
        }
    }

    /**
     * This will perform the pre liquibase upgrade. It will execute the upgrade scripts in order to get to the 8.3.pre
     * version of the database so that liquibase can be applied.
     *
     * @param databaseConfig  The database to upgrade
     * @param conn            The connection to use to upgrade the database
     * @param dbScriptsFolder The db upgrade scripts folder
     * @param ui              The ui to use for messages
     * @return The legacy upgrade result
     * @throws IOException
     */
    @NotNull
    public DBActionsResult legacyUpgrade(@NotNull final DatabaseConfig databaseConfig, @NotNull final Connection conn, @NotNull final File dbScriptsFolder,
                                          @Nullable final DBActionsListener ui) throws IOException {
        Statement stmt = null;
        try {
            //get the upgrade file map
            final Map<String, String[]> upgradeMap = DbUpgradeUtil.buildUpgradeMap(dbScriptsFolder);

            final String databaseName = databaseConfig.getName();
            String oldVersion = getDbVersion(conn);
            stmt = conn.createStatement();
            //upgrade to 8.3.pre version
            while (!LiquibaseDBManager.JAVELIN_PRE_DB_VERSION.equals(oldVersion)) {
                final String[] upgradeInfo = upgradeMap.get(oldVersion);
                if (upgradeInfo == null) {
                    final String msg = "No upgrade path from \"" + oldVersion + "\"";
                    logger.warning(msg);
                    return new DBActionsResult(StatusType.CANNOT_UPGRADE, msg, null);
                } else {
                    logger.info("Upgrading \"" + databaseName + "\" from " + oldVersion + "->" + upgradeInfo[0]);

                    String upgradeFile = upgradeInfo[1];
                    if (upgradeInfo[2] != null) {
                        upgradeFile = upgradeFile.substring(0, upgradeFile.lastIndexOf('_')) + "_" + DbUpgradeUtil.UPGRADE_TRY_SUFFIX + ".sql";
                    }
                    String[] statements = DbUpgradeUtil.getStatementsFromFile(upgradeFile);

                    if (ui != null) {
                        ui.showSuccess("Upgrading \"" + databaseConfig.getName() + "\" from " + oldVersion + "->" + upgradeInfo[0] + EOL_CHAR);
                        ui.showSuccess("Please note that if you have a lot of audits, this may take a long time." + EOL_CHAR);
                    }
                    String lastStatementAttempted = null;
                    try {
                        for (String statement : statements) {
                            lastStatementAttempted = statement;
                            stmt.executeUpdate(statement);
                        }
                    } catch (SQLException e) {
                        if (upgradeInfo[2] != null) {

                            upgradeFile = upgradeFile.substring(0, upgradeFile.lastIndexOf('_')) + "_" + DbUpgradeUtil.UPGRADE_SUCCESS_SUFFIX + ".sql";
                            if (ui != null) {
                                ui.showSuccess("upgrade fail, checking success." + EOL_CHAR);
                            }
                            statements = DbUpgradeUtil.getStatementsFromFile(upgradeFile);
                            Statement statement = conn.createStatement();
                            for (int i = 0; i < statements.length - 1; ++i) {
                                String sql = statements[i];
                                if (logger.isLoggable(Level.FINE)) {
                                    logger.log(Level.FINE, "Running statement: " + sql);
                                }
                                statement.executeUpdate(sql);
                            }

                            String lastLine = statements[statements.length - 1];
                            lastLine = lastLine.replace(DB_NAME_VAR, databaseName);
                            logger.log(Level.FINE, "Running statement: " + lastLine);
                            ResultSet result = statement.executeQuery(lastLine);
                            logger.fine("Statement: " + statements[0]);
                            if (!result.next()) {
                                throw new SQLException("Error checking success, no result returned.");
                            }
                        } else {
                            logger.log(Level.INFO, "Last SQL statement attempted: " + lastStatementAttempted);
                            throw e;
                        }
                    }

                    if (ui != null)
                        ui.showSuccess("Completed upgrade from " + oldVersion + "->" + upgradeInfo[0] + EOL_CHAR);

                    oldVersion = getDbVersion(conn);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error during upgrade.", e);
            return new DBActionsResult(determineErrorStatus(e.getSQLState()), ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
        return new DBActionsResult(StatusType.SUCCESS);
    }

    private StatusType determineErrorStatus(String sqlState) {
        if (StringUtils.equals(ERROR_CODE_UNKNOWNDB, sqlState))
            return StatusType.UNKNOWNDB_FAILURE;
        else if (StringUtils.equals(ERROR_CODE_AUTH_FAILURE, sqlState))
            return StatusType.AUTHORIZATION_FAILURE;
        else if (StringUtils.equals("08S01", sqlState))
            return StatusType.UNKNOWNHOST_FAILURE;
        else
            return StatusType.UNKNOWN_FAILURE;
    }

    private StatusType determineErrorStatus(LiquibaseException e) {
        if (e instanceof DatabaseException && e.getCause() instanceof SQLException) {
            return determineErrorStatus(((SQLException) e.getCause()).getSQLState());
        } else {
            return StatusType.ERROR;
        }
    }

    private void copyDbSchema(@NotNull final Connection sourceConnection, @NotNull final Connection targetConnection) throws SQLException {
        final String[] createStatements = getDbCreateStatementsFromDb(sourceConnection);
        executeUpdates(targetConnection, createStatements);
    }

    private void copyFunctions(@NotNull final Connection sourceConnection, @NotNull final Connection targetConnection) throws SQLException {
        String[] createStatements = getDbCreateFunctionStatementsFromDb(sourceConnection);
        executeUpdates(targetConnection, createStatements);
    }

    private void copyDatabaseContents(@NotNull final Connection privSourceConn, @NotNull final Connection privDestConn, final boolean skipAudits) throws SQLException {
        Statement stmt = null;
        try {
            stmt = privDestConn.createStatement();
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            getDbDataStatements(privSourceConn, skipAudits, new StatementUser() {
                @Override
                public void useStatement(@NotNull final String tableName, @NotNull final List<Object> rowData) throws SQLException {
                    int size = rowData.size();
                    StringBuilder sql = new StringBuilder(512);
                    sql.append("insert into ").append(tableName).append(
                            " values (").append(StringUtils.repeat("?, ", size - 1)).append('?').append(");");

                    //noinspection JDBCPrepareStatementWithNonConstantString
                    PreparedStatement pStmt = null;
                    try {
                        pStmt = privDestConn.prepareStatement(sql.toString());
                        for (int i = 0; i < size; i++) {
                            pStmt.setObject(i + 1, rowData.get(i));
                        }
                        pStmt.addBatch();
                        pStmt.executeUpdate();
                    } finally {
                        ResourceUtils.closeQuietly(pStmt);
                    }
                }
            });
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
    }


    private String hidepass(String password) {
        // We will only indicate whether or not the password is empty here.
        // To avoid leaking information about the length we won't just mask the characters;
        // to avoid misleadingly implying an incorrect length, we won't just write asterixes
        return password == null || password.length() < 1 ? "<empty>" : "<not shown>";
    }

    private interface StatementUser {
        void useStatement(@NotNull final String tableName, @NotNull final List<Object> rowData) throws SQLException;
    }

    private void getDbDataStatements(@NotNull final Connection connection, final boolean skipAudits, @NotNull final StatementUser statementUser) throws SQLException {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            Set<String> tableNames = getTableNames(connection);

            for (String tableName : tableNames) {
                //skip audits
                if (tableName.toLowerCase().startsWith("audit") && skipAudits) continue;

                ResultSet dataRs = stmt.executeQuery("select * from " + tableName);
                ResultSetMetaData meta = dataRs.getMetaData();
                while (dataRs.next()) {
                    List<Object> rowData = new ArrayList<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        rowData.add(dataRs.getObject(i));
                    }
                    statementUser.useStatement(tableName, rowData);
                }
            }
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
    }

    public static enum StatusType {
        SUCCESS(0),
        ALREADY_EXISTS(2),
        CREATEFILE_MISSING(3),
        INCORRECT_VERSION(4),
        ERROR(5),
        AUTHORIZATION_FAILURE(28000),
        UNKNOWNDB_FAILURE(42000),
        UNKNOWN_FAILURE(-1),
        CHECK_INTERNAL_ERROR(-2),
        UNKNOWNHOST_FAILURE(-3),
        CANNOT_UPGRADE(-4);

        public int getCode() {
            return code;
        }

        private StatusType(int code) {
            this.code = code;
        }

        private final int code;
    }

    public static class DBActionsResult {
        private final StatusType status;
        private final String errorMessage;
        private final Throwable thrown;

        public DBActionsResult(StatusType status) {
            this.status = status;
            this.errorMessage = null;
            this.thrown = null;
        }

        public DBActionsResult(StatusType statusType, String message, Throwable thrown) {
            this.status = statusType;
            this.errorMessage = message;
            this.thrown = thrown;
        }

        public StatusType getStatus() {
            return status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Throwable getThrown() {
            return thrown;
        }
    }

    static final class DBPermission {
        @NotNull
        private final DatabaseConfig databaseConfig;
        @Nullable
        private final Set<String> hosts;
        private final boolean isGrant;

        /**
         * Create a new DBPermission
         *
         * @param databaseConfig the DBInformation object that contains the information for the permission to be
         *                       generated
         * @param hosts          the database hosts for the cluster
         * @param isGrant        true if this is a grant, false if this is a revocation
         */
        DBPermission(@NotNull final DatabaseConfig databaseConfig, @Nullable final Set<String> hosts, final boolean isGrant) {
            this.databaseConfig = databaseConfig;
            this.hosts = hosts;
            this.isGrant = isGrant;
        }

        @NotNull
        public String getPermissionStatement(@NotNull final String hostname) {
            return (isGrant ? SQL_GRANT_ALL : SQL_REVOKE_ALL) +
                    databaseConfig.getName() + ".* " +
                    (isGrant ? "to " : "from ") +
                    databaseConfig.getNodeUsername() + "@'" + hostname + "'" +
                    (isGrant ? " identified by '" + databaseConfig.getNodePassword() + "'" : "");
        }

        /**
         * Due to MySQL having grants with empty username for the db host, we need to
         * ensure we add a specific grant for the db user for each db host (since the
         * host is used to check the grants before the user is compared)
         */
        @NotNull
        private String[] getStatements() {
            final List<String> list = new ArrayList<>();
            list.add(getPermissionStatement("%"));


            final Set<String> allhosts = new HashSet<>();
            allhosts.add(databaseConfig.getHost());
            if (hosts != null) {
                allhosts.addAll(hosts);
            }

            for (String host : allhosts) {
                host = host.trim();

                //grant the ACTUAL hostname, not the localhost one
                try {
                    if (!host.equals("localhost") &&
                            !host.equals("localhost.localdomain") &&
                            !InetAddressUtil.isLoopbackAddress(host)) {
                        // add pre-canonicalized
                        list.add(getPermissionStatement(host));

                        // add canonical
                        list.add(getPermissionStatement(InetAddress.getByName(host).getCanonicalHostName()));
                    }
                } catch (UnknownHostException uhe) {
                    logger.warning("Could not resolve canonical hostname for '" + host + "'.");
                }
            }

            list.add(getPermissionStatement("localhost"));
            list.add(getPermissionStatement("localhost6"));
            list.add(getPermissionStatement("localhost.localdomain"));
            list.add(getPermissionStatement("localhost6.localdomain6"));
            list.add("FLUSH PRIVILEGES");

            return list.toArray(new String[list.size()]);
        }
    }

    class ProgressTimerTask extends TimerTask {
        private final DBActionsListener ui;

        ProgressTimerTask(DBActionsListener ui) {
            this.ui = ui;
        }


        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            ui.showSuccess(". ");
        }
    }
}
