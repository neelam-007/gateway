package com.l7tech.gateway.config.manager.db;

import com.l7tech.util.BuildInfo;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.CausedIOException;
import com.l7tech.gateway.config.manager.LicenseChecker;
import com.l7tech.server.management.config.node.DatabaseConfig;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.PasswordAuthentication;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Various DB manipulaton and checking methods used by the configuration wizard. Can probably be made more generic to be
 * usefult others as well.
 *
 * User: megery
 */
public class DBActions {
    private static final Logger logger = Logger.getLogger(DBActions.class.getName());
    private static final String EOL_CHAR = System.getProperty("line.separator");
    private static final String DEFAULT_DB_URL = "jdbc:mysql://{0}:{1}/{2}?autoReconnect=false&characterEncoding=UTF8&characterSetResults=UTF8&socketTimeout=120000&connectTimeout=10000";

    public static final int DB_SUCCESS = 0;
    public static final int DB_ALREADY_EXISTS = 2;
    public static final int DB_CREATEFILE_MISSING = 3;
    public static final int DB_INCORRECT_VERSION = 4;
    public static final int DB_ERROR = 5;

    public static final int DB_AUTHORIZATION_FAILURE = 28000;
    public static final int DB_UNKNOWNDB_FAILURE = 42000;

    public static final int DB_UNKNOWN_FAILURE = -1;
    public static final int DB_CHECK_INTERNAL_ERROR = -2;
    public static final int DB_UNKNOWNHOST_FAILURE = -3;
    public static final int DB_CANNOT_UPGRADE = -4;

    public static final String MYSQL_CLASS_NOT_FOUND_MSG = "Could not locate the mysql driver in the classpath. Please check your classpath and rerun the wizard";
    public static final String GENERIC_DBCREATE_ERROR_MSG = "There was an error while attempting to create the database. Please try again";
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
        } catch( ClassNotFoundException cnfe ) {
            logger.info("MySQL JDBC driver not available.");        
        }
    }

    private CheckSSGDatabase ssgDbChecker;

    private DbVersionChecker[] dbCheckers = new DbVersionChecker[] {
        new DbVersion47Checker(),
        new DbVersion46Checker(),
        new DbVersion45Checker(),
        new DbVersion44Checker(),
        new DbVersion43Checker(),
        new DbVersion42Checker(),
        new DbVersion40Checker(),
        new DbVersion37Checker(),
        new DbVersion365Checker(),
        new DbVersion36Checker(),
        new DbVersion35Checker(),
        new DbVersion34Checker(),
        new DbVersion33Checker(),
        new DbVersion3132Checker(),
    };

    private static final String UPGRADE_SQL_PATTERN = "^upgrade_(.*)-(.*).sql$";
    private LicenseChecker licChecker;

    //
    // CONSTRUCTOR
    //
    public DBActions() {
        init();
    }

    //
    // PUBLIC METHODS
    //

    /**
     * Retrieve a database connection to the given database. This connection will need to be closed by the caller to
     * avoid resource leaks
     * @param dburl a URL of the form jdbc:dbtype://host/dbname where dbtype is the name of the db driver/vendor
     * (in our case it's mysql)
     * @param dbuser the user to connect with
     * @param dbpasswd the password for the db user. Not null, pass "" for no password
     * @return an established connection to the DB specified in dburl. Caller is responsible for closing and
     * maintinaining this connection
     * @throws SQLException if there was an exception while connecting to the DB
     */
    public Connection getConnection(String dburl, String dbuser, String dbpasswd) throws SQLException {
        return DriverManager.getConnection(dburl, dbuser, dbpasswd);
    }

    public DBActionsResult createDb(DatabaseConfig config, Set<String> hosts, String dbCreateScript, boolean overwriteDb) throws IOException {
        DBActionsResult result = new DBActionsResult();

        Connection conn = null;
        try {
            conn = getConnection(config, true);

            if (testForExistingDb(conn, config.getName())) {
                if (!overwriteDb)
                    result.setStatus(DB_ALREADY_EXISTS);
                else {
                    //we should overwrite the db
                    dropDatabase( conn, config.getName(), false );
                    createDatabaseWithGrants( conn, config, hosts );
                    createTables( config, dbCreateScript );
                    result.setStatus(DB_SUCCESS);
                }
            } else {
                createDatabaseWithGrants( conn, config, hosts );
                createTables( config, dbCreateScript );
                result.setStatus(DB_SUCCESS);
            }
        } catch (SQLException e) {
            result.setStatus(determineErrorStatus(e.getSQLState()));
            result.setErrorMessage(ExceptionUtils.getMessage(e));

            logger.warning("Could not create database. An exception occurred." + ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly(conn);
        }

        return result;
    }


    public DBActionsResult upgradeDbSchema(DatabaseConfig databaseConfig,
                                           String oldVersion,
                                           String newVersion,
                                           String schemaFilePath,
                                           final DBActionsListener ui) throws IOException {
        DBActionsResult result = new DBActionsResult();
        File file = new File(schemaFilePath);

        Map<String, String[]> upgradeMap = buildUpgradeMap(file.getParentFile());

        Connection conn = null;
        Statement stmt = null;

        TimerTask spammer = null;
        try {
            String databaseName = databaseConfig.getName();
            conn = getConnection(databaseConfig, false);
            stmt = conn.createStatement();
            while (!oldVersion.equals(newVersion)) {
                String[] upgradeInfo = upgradeMap.get(oldVersion);
                if (upgradeInfo == null) {
                    String msg = "no upgrade path from \"" + oldVersion + "\" to \"" + newVersion + "\"";
                    logger.warning(msg);
                    result.setStatus(DB_CANNOT_UPGRADE);
                    result.setErrorMessage(msg);
                    break;
                } else {
                    logger.info("Upgrading \"" + databaseName + "\" from " + oldVersion + "->" + upgradeInfo[0]);

                    String[] statements = getCreateDbStatementsFromFile(upgradeInfo[1]);

                    spammer = new ProgressTimerTask(ui);

                    if (ui != null) {
                        ui.showSuccess("Upgrading \"" + databaseName + "\" from " + oldVersion + "->" + upgradeInfo[0] + EOL_CHAR) ;
                        ui.showSuccess("Please note that if you have a lot of audits, this may take a long time." + EOL_CHAR) ;
                        Background.scheduleRepeated(spammer, 2000, 2000);
                    }

                    conn.setAutoCommit(false);
                    for (String statement : statements) {
                        stmt.executeUpdate(statement);
                    }

                    conn.commit();
                    conn.setAutoCommit(true);

                    if (spammer != null) {
                        Background.cancel(spammer);
                        spammer = null;
                    }
                    if (ui != null) ui.showSuccess("completed" + EOL_CHAR);


                    oldVersion = checkDbVersion(conn);
                }
            }
            if (ui != null) ui.showSuccess(databaseName + " was upgraded successfully." + EOL_CHAR);

        } catch (SQLException e) {
            result.setStatus(determineErrorStatus(e.getSQLState()));
            result.setErrorMessage(ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly(stmt);
            ResourceUtils.closeQuietly(conn);
            if (spammer != null) {
                Background.cancel(spammer);
            }
        }

        return result;
    }

    public boolean doCreateDb(DatabaseConfig config, Set<String> hosts, String schemaFilePath, boolean overwriteDb, DBActionsListener ui) {
        DatabaseConfig databaseConfig = new DatabaseConfig( config );
        String pUsername = databaseConfig.getDatabaseAdminUsername();
        String pPassword = databaseConfig.getDatabaseAdminPassword();

        //check if the root username is "" or null, or the password is null. Password is allowed to be "", if there isn't a password
        while (StringUtils.isEmpty(pUsername) || pPassword == null) {
            if (ui != null) {
                String defaultUserName = StringUtils.isEmpty(pUsername)?"root":pUsername;
                PasswordAuthentication passwordCreds = ui.getPrivelegedCredentials(
                    "Please enter the credentials for the root database user (needed to create a database)",
                    "Please enter the username for the root database user (needed to create a database): [" + defaultUserName + "] ",
                    "Please enter the password for the root database user: ",
                    defaultUserName);

                if (passwordCreds == null) {
                    return false;
                } else {
                    pUsername = passwordCreds.getUserName();
                    pPassword = new String(passwordCreds.getPassword());
                }
            }
        }

        databaseConfig.setDatabaseAdminUsername( pUsername );
        databaseConfig.setDatabaseAdminPassword( pPassword );

        String errorMsg;
        boolean isOk = false;

        DBActionsResult result;
        try {
            logger.info("Attempting to create a new database (" + databaseConfig.getHost() + "/" + databaseConfig.getName() + ") using privileged user \"" + pUsername + "\"");

            result = createDb(databaseConfig,
                              hosts,
                              schemaFilePath,
                              overwriteDb);

            int status = result.getStatus();
            if ( status == DBActions.DB_SUCCESS) {
                isOk = true;
                if (ui != null)
                    ui.showSuccess("Database Successfully Created\n");
            } else {
                switch (status) {
                    case DBActions.DB_UNKNOWNHOST_FAILURE:
                        errorMsg = "Could not connect to the host: \"" +  databaseConfig.getHost() + "\". Please check the hostname and try again.";
                        logger.info("Connection to the database for creating was unsuccessful - see warning/errors for details");
                        logger.warning(errorMsg);
                        if (ui != null) ui.showErrorMessage(errorMsg);
                        isOk = false;
                        break;
                    case DBActions.DB_AUTHORIZATION_FAILURE:
                        errorMsg = "There was an authentication error when attempting to create the new database using the username \"" +
                                pUsername + "\". Perhaps the password is wrong. Please retry.";
                        logger.info("Connection to the database for creating was unsuccessful - see warning/errors for details");
                        logger.warning(errorMsg);
                        if (ui != null) ui.showErrorMessage(errorMsg);
                        isOk = false;
                        break;
                    case DBActions.DB_ALREADY_EXISTS:
                        logger.warning("The database named \"" +  databaseConfig.getName() + "\" already exists");
                        if (ui != null) {
                            if (ui.getOverwriteConfirmationFromUser(databaseConfig.getName())) {
                                logger.info("creating new database (overwriting existing one)");
                                logger.warning("The database will be overwritten");
                                isOk = doCreateDb(databaseConfig, hosts, schemaFilePath, true, ui);
                            }
                        } else {
                            isOk = false;
                        }
                        break;
                    case DBActions.DB_UNKNOWN_FAILURE:
                    default:
                        errorMsg = GENERIC_DBCREATE_ERROR_MSG + ": " + result.getErrorMessage();
                        logger.warning(errorMsg);
                        if (ui != null) ui.showErrorMessage(errorMsg);
                        isOk = false;
                        break;
                }
            }
        } catch (IOException e) {
            errorMsg = "Could not create the database because there was an error while reading the file \"" + schemaFilePath + "\"." +
                    " The error was: " + ExceptionUtils.getMessage(e);
            logger.warning(errorMsg);
            if (ui != null) ui.showErrorMessage(errorMsg);
            isOk = false;
        }

        return isOk;
    }

    private String hidepass(String password) {
        // We will only indicate whether or not the password is empty here.
        // To avoid leaking information about the length we won't just mask the characters;
        // to avoid misleadingly implying an incorrect length, we won't just write asterixes
        return password == null || password.length() < 1 ? "<empty>" : "<not shown>";
    }

    public boolean doExistingDb(DatabaseConfig config, String schemaFilePath, String currentVersion, DBActionsListener ui) {
        String errorMsg;
        boolean isOk;

        DatabaseConfig databaseConfig = new DatabaseConfig(config);
        String hostname = databaseConfig.getHost();
        String dbName = databaseConfig.getName();
        String username = databaseConfig.getNodeUsername();
        String password = databaseConfig.getNodePassword();
        String privUsername = databaseConfig.getDatabaseAdminUsername();
        String privPassword = databaseConfig.getDatabaseAdminPassword();

        logger.info("Attempting to connect to an existing database (" + hostname + "/" + dbName + ")" + "using username/password " + username + "/" + hidepass(password));

        DBActions.DBActionsResult status = checkExistingDb(databaseConfig);
        if (status.getStatus() == DBActions.DB_SUCCESS) {
            logger.info(CONNECTION_SUCCESSFUL_MSG);

            if (licChecker != null && !checkLicense(ui, currentVersion, databaseConfig))
                return false;

            logger.info("Now Checking database version.");
            String dbVersion = checkDbVersion(databaseConfig);
            if (dbVersion == null) {
                errorMsg = "The " + dbName + " database does not appear to be a valid SSG database.";
                logger.warning(errorMsg);
                if (ui != null) ui.showErrorMessage(errorMsg);
                isOk = false;
            } else {
                if (dbVersion.equals(currentVersion)) {
                    logger.info("Database version is correct (" + dbVersion + ")");
                    if (ui != null) ui.hideErrorMessage();
                    isOk = true;
                }
                else {
                    errorMsg = "The current database version (" + dbVersion+ ") is incorrect (needs to be " + currentVersion + ") and needs to be upgraded" ;
                    logger.warning(errorMsg);
                    boolean shouldUpgrade = false;
                    if (ui != null) {
                        String msg = "The \"" + dbName + "\" database appears to be a " + dbVersion + " database and needs to be upgraded to " + currentVersion + "\n" +
                            "Would you like to attempt an upgrade?";
                        shouldUpgrade = ui.getGenericUserConfirmation(msg);
                    }
                    if (shouldUpgrade) {
                        try {
                            PasswordAuthentication creds = null;
                            if (StringUtils.isEmpty(privUsername) || StringUtils.isEmpty(privPassword)) {
                                creds = ui.getPrivelegedCredentials(
                                        "Please enter the credentials for the root database user (needed to upgrade the database)",
                                        "Please enter the username for the root database user (needed to upgrade the database): [root]",
                                        "Please enter the password for root database user (needed to upgrade the database): ", "root");
                            }
                            if (creds == null) return false;

                            privUsername = creds.getUserName();
                            config.setDatabaseAdminUsername(privUsername);

                            privPassword = new String(creds.getPassword());
                            config.setDatabaseAdminPassword(privPassword);

                            isOk = doDbUpgrade(config, schemaFilePath, currentVersion, dbVersion, ui);
                            if (isOk) ui.showSuccess("The database was successfully upgraded\n");
                        } catch (IOException e) {
                            errorMsg = "There was an error while attempting to upgrade the database";
                            logger.severe(errorMsg);
                            logger.severe(ExceptionUtils.getMessage(e));
                            ui.showErrorMessage(errorMsg);
                            isOk = true;
                        }
                    } else {
                        if (ui != null) ui.showErrorMessage("The database must be a correct version before proceeding.");
                        isOk = false;
                    }
                }
            }
        } else {
            switch (status.getStatus()) {
                case DBActions.DB_UNKNOWNHOST_FAILURE:
                    errorMsg = "Could not connect to the host: \"" + hostname + "\". Please check the hostname and try again.";
                    logger.info("Connection to the database for creating was unsuccessful - see warning/errors for details");
                    logger.warning(errorMsg);
                    if (ui != null) ui.showErrorMessage(errorMsg);
                    isOk = false;
                    break;
                case DBActions.DB_AUTHORIZATION_FAILURE:
                    logger.info(CONNECTION_UNSUCCESSFUL_MSG);
                    errorMsg = MessageFormat.format("There was a connection error when attempting to connect to the database \"{0}\" using the username \"{1}\". " +
                            "Perhaps the password is wrong. Either the username and/or password is incorrect, or the database \"{2}\" does not exist.",
                            dbName, username, dbName);
                    if (ui != null) ui.showErrorMessage(errorMsg);
                    logger.warning("There was an authentication error when attempting to connect to the database \"" + dbName + "\" using the username \"" +
                            username + "\" and password \"" + password + "\".");
                    isOk = false;
                    break;
                case DBActions.DB_UNKNOWNDB_FAILURE:
                    logger.info(CONNECTION_UNSUCCESSFUL_MSG);
                    errorMsg = "Could not connect to the database \"" + dbName + "\". The database does not exist or the user \"" + username + "\" does not have permission to access it." +
                            "Please check your input and try again.";
                    if (ui != null) ui.showErrorMessage(errorMsg);
                    logger.warning("Could not connect to the database \"" + dbName + "\". The database does not exist.");
                    isOk = false;
                    break;
                default:
                    logger.info(CONNECTION_UNSUCCESSFUL_MSG);
                    errorMsg = GENERIC_DBCONNECT_ERROR_MSG;
                    if (ui != null) ui.showErrorMessage(errorMsg);
                    logger.warning("There was an unknown error while attempting to connect to the database.");
                    isOk = false;
                    break;
            }
        }
        return isOk;
    }

    private boolean checkLicense(DBActionsListener ui, String currentVersion, DatabaseConfig databaseConfig) {
        logger.info("Now Checking SSG License validity");
        Connection conn = null;
        try {
            try {
                conn = getConnection(databaseConfig, false);
            } catch (SQLException e) {
                logger.info("Cannot check the license. Could not get a connection to the database");
                return false;
            }
            try {
                licChecker.checkLicense(conn, currentVersion, BuildInfo.getProductName(), BuildInfo.getProductVersionMajor(), BuildInfo.getProductVersionMinor());
                logger.info("The License is valid and will work with this version (" + currentVersion + ").");
            } catch ( InvalidLicenseException e) {
                String message = ExceptionUtils.getMessage(e);
                logger.warning(message);
                if (ui != null) {
                    if (!ui.getGenericUserConfirmation(message + "\nDo you wish to continue?")) {
                        ui.showErrorMessage(message);
                        return false;
                    }
                }
            }
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
        return true;
    }

    /**
     * Get a regular or admin connection.
     *
     * WARNING: admin connections do not connect to the database named in the config! 
     */
    public Connection getConnection( final DatabaseConfig databaseConfig, boolean admin ) throws SQLException {
        return getConnection(
                databaseConfig.getHost(),
                databaseConfig.getPort(),
                admin ? ADMIN_DB_NAME : databaseConfig.getName(),
                admin ? databaseConfig.getDatabaseAdminUsername() : databaseConfig.getNodeUsername(),
                admin ? databaseConfig.getDatabaseAdminPassword() : databaseConfig.getNodePassword());
    }

    public Connection getConnection(String hostname, int port, String dbName, String username, String password) throws SQLException {
        return DriverManager.getConnection(makeConnectionString(hostname, port, dbName), username, password);
    }

    public boolean dropDatabase( DatabaseConfig databaseConfig, Set<String> hosts, boolean isInfo, boolean doRevoke, DBActionsListener ui ) {
        String pUsername = databaseConfig.getDatabaseAdminUsername();
        String pPassword = databaseConfig.getDatabaseAdminPassword();
        if (StringUtils.isEmpty(pUsername) || pPassword == null) {
            if (ui != null) {
                String defaultUserName = StringUtils.isEmpty(pUsername)?"root":pUsername;
                PasswordAuthentication passwordCreds = ui.getPrivelegedCredentials(
                    "Please enter the credentials for the root database user (needed to drop the database)",
                    "Please enter the username for the root database user: [" + defaultUserName + "] ",
                    "Please enter the password for the root database user: ",
                    defaultUserName);

                if (passwordCreds == null) {
                    return false;
                } else {
                    pUsername = passwordCreds.getUserName();
                    pPassword = new String(passwordCreds.getPassword());
                }
            }
        }

        boolean allIsWell = false;
        Connection conn = null;
        Statement stmt = null;
        try {
            DatabaseConfig adminConfig = new DatabaseConfig( databaseConfig );
            adminConfig.setDatabaseAdminUsername( pUsername );
            adminConfig.setDatabaseAdminPassword( pPassword );

            conn = getConnection( adminConfig, true );
            try {
                dropDatabase( conn, databaseConfig.getName(), isInfo );
                allIsWell = true;
            } catch (SQLException e) {
                logger.severe("Failure while dropping the database: " + ExceptionUtils.getMessage(e));
                allIsWell = false;
            }

            if (allIsWell) {
                if (doRevoke) {
                    stmt = conn.createStatement();
                    String [] revokeStatements = getRevokeStatements(adminConfig, hosts);
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

//
// PRIVATE METHODS
//

    private void dropDatabase(Connection connection, String dbName, boolean isInfo) throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(SQL_DROP_DB + dbName);

            if (isInfo)
                logger.info("dropping database \"" + dbName + "\"");
            else
                logger.warning("dropping database \"" + dbName + "\"");
        } finally {
            ResourceUtils.closeQuietly(statement);    
        }
    }

    private DBActionsResult checkExistingDb(DatabaseConfig databaseConfig) {

        DBActionsResult result = new DBActionsResult();
        Connection conn = null;

        try {
            conn = getConnection(databaseConfig, false);
            result.setStatus(DB_SUCCESS);
        } catch (SQLException e) {
            String message = ExceptionUtils.getMessage(e);
            result.setStatus(determineErrorStatus(e.getSQLState()));
            result.setErrorMessage(message);

            logger.warning("Could not login to the database using " + databaseConfig.getNodeUsername() + ":" + hidepass(databaseConfig.getNodePassword()) +  "@" + databaseConfig.getHost() + ":" + databaseConfig.getPort() + "/" + databaseConfig.getName());
            logger.warning(message);
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
        
        return result;
    }

    private void init() {
        ssgDbChecker = new CheckSSGDatabase();
        //always sort the dbCheckers in reverse in case someone has added one out of sequence so things still work properly
        Arrays.sort(dbCheckers, Collections.reverseOrder());

        if (!hasCheckForCurrentVersion(dbCheckers)) {
            DbVersionChecker[] checkers = new DbVersionChecker[dbCheckers.length+1];
            System.arraycopy(dbCheckers, 0, checkers, 1, dbCheckers.length);
            checkers[0] =  new DbVersionBeHappyChecker(dbCheckers.length > 0 ? dbCheckers[0] : null);
            dbCheckers = checkers;
        }
    }

    private boolean hasCheckForCurrentVersion(DbVersionChecker[] checkers) {
        boolean hasCurrent = false;
        String currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();
        if (StringUtils.isNotEmpty(BuildInfo.getProductVersionSubMinor())) {
            currentVersion += "." + BuildInfo.getProductVersionSubMinor();
        }

        for (DbVersionChecker checker : checkers) {
            if (currentVersion.equals(checker.getVersion())) {
                hasCurrent = true;
                break;
            }
        }

        return hasCurrent;
    }

    private Set<String> getTableColumns(String tableName, DatabaseMetaData metadata) throws SQLException {
        Set<String> columns = new HashSet<String>();
        ResultSet rs;
        rs = metadata.getColumns(null, "%", tableName, "%");
        while(rs.next()) {
            columns.add(rs.getString("COLUMN_NAME").toLowerCase());
        }
        return columns;
    }

    //will return the version of the DB, or null if it cannot be determined
    private String getDbVersion(Map<String, Set<String>> tableData) {
        String version = null;
        //make sure we're even dealing with something that smells like an SSG database
        if (ssgDbChecker.doCheck(tableData)) {
            //this is an SSG database, now determine the exact version
            int dbCheckIndex = 0;
            boolean versionFound;
            do {
                DbVersionChecker checker = dbCheckers[dbCheckIndex++];
                if (versionFound = checker.doCheck(tableData)) {
                     version = checker.getVersion();
                }

            } while(!versionFound && dbCheckIndex < dbCheckers.length);
         }
         return version;
     }

    public String checkDbVersion(final DatabaseConfig databaseConfig) {
        Connection conn = null;
        String dbVersion = null;
        try {
            conn = getConnection(databaseConfig, false);
            dbVersion = checkDbVersion(conn);
        } catch (SQLException e) {
            logger.warning("Error while checking the database version: " + ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
        return dbVersion;
    }

    //checks the database version heuristically by looking for table names, columns etc. known to have been introduced
    //in particular versions. The checks are, for the most part, self contained and the code is designed to be extensible.
    private String checkDbVersion(Connection conn) throws SQLException {
        Map<String, Set<String>> tableData = collectMetaInfo(conn);
        //now we have a hashtable of tables and their columns
        return getDbVersion(tableData);
    }

    private Set<String> getTableNames(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            Set<String> tables = new LinkedHashSet<String>();

            DatabaseMetaData metadata = conn.getMetaData();
            String[] tableTypes = {
                    "TABLE",
            };

            ResultSet tableNames = metadata.getTables(null, "%", "%", tableTypes);
            while (tableNames.next()) {
                String tableName = tableNames.getString("TABLE_NAME");
                tables.add(tableName);
            }
            return tables;
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
    }

    private Map<String, Set<String>> collectMetaInfo(Connection conn) throws SQLException {
        Map<String, Set<String>> tableData = new HashMap<String, Set<String>>();

        Set<String> tables = getTableNames(conn);
        for (String tableName : tables) {
            Set<String> columns = getTableColumns(tableName, conn.getMetaData());
            if (columns != null)
                tableData.put(tableName.toLowerCase(), columns);
        }
        return tableData;
    }

    /**
     * Create DB and do grants, DB will be empty 
     */
    private void createDatabaseWithGrants(Connection connection, DatabaseConfig databaseConfig, Set<String> hosts) throws SQLException, IOException {
        if ( "".equals(databaseConfig.getNodePassword()) ) {
            throw new CausedIOException("Cannot create database with empty password for user '"+databaseConfig.getNodeUsername()+"'.");            
        }

        connection.setAutoCommit(false); //start transaction
        String newDbName = databaseConfig.getName();

        logger.info("creating database \"" + newDbName +"\"");

        createDatabase( connection, newDbName);

        String[] grantSql = getGrantStatements(databaseConfig, hosts);
        if ( grantSql != null ) {
            logger.info( "Creating user \"" + databaseConfig.getNodeUsername() + "\" and performing grants on " + newDbName + " database" );
            executeUpdates( connection, grantSql );
        }

        connection.commit();     //finish transaction
        connection.setAutoCommit(true);
    }

    private void createDatabase( DatabaseConfig databaseConfig ) throws SQLException {
        Connection connection = null;
        Statement stmt = null;
        try {
            connection = getConnection(databaseConfig, true);
            stmt = connection.createStatement();
            stmt.executeUpdate( SQL_CREATE_DB + databaseConfig.getName() );
        } finally {
            ResourceUtils.closeQuietly(stmt);
            ResourceUtils.closeQuietly(connection);
        }
    }

    private void createDatabase( Connection dbConnection, String dbname ) throws SQLException {
        Statement stmt = null;
        try {
            stmt = dbConnection.createStatement();
            stmt.executeUpdate(SQL_CREATE_DB + dbname);
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
    }

    private void createTables( DatabaseConfig databaseConfig, String dbCreateScript ) throws SQLException, IOException {
        if ( dbCreateScript != null ) {
            String[] sql = getCreateDbStatementsFromFile(dbCreateScript);
            if ( sql != null ) {
                logger.info( "Creating schema for " + databaseConfig.getName() + " database" );

                Connection connection = null;
                try {
                    connection = getConnection(databaseConfig, false);
                    executeUpdates( connection, sql );
                } finally {
                    ResourceUtils.closeQuietly( connection );
                }
            }
        } else {
            logger.info("Skipping creation of tables and rows");
        }
    }

    public String[] getGrantStatements(DatabaseConfig databaseConfig, Set<String> hosts) {
        return getPermissionChangeStatements(databaseConfig, hosts, true);
    }

    private String[] getRevokeStatements(DatabaseConfig databaseConfig, Set<String> hosts) {
        return getPermissionChangeStatements(databaseConfig, hosts, false);
    }

    private void executeUpdates( Connection connection, String[] sqlStatements) throws SQLException {
        Statement statement = null;

        if ( sqlStatements != null ) {
            try {
                statement = connection.createStatement();

                for ( String statementSql : sqlStatements) {
                    statement.executeUpdate( statementSql );
                }
            } finally {
                ResourceUtils.closeQuietly( statement );
            }
        }
    }

    private String[] getPermissionChangeStatements(DatabaseConfig databaseConfig, Set<String> hosts,  boolean doGrants) {
        return new DBPermission(databaseConfig, hosts, doGrants).getStatements();
    }

    private String[] getCreateDbStatementsFromFile(String dbCreateScript) throws IOException {
        String []  stmts = null;
        BufferedReader reader = null;
        try {
            StringBuffer sb = new StringBuffer();

            reader = new BufferedReader(new FileReader(dbCreateScript));
            String temp;
            while((temp = reader.readLine()) != null) {
                if (!temp.startsWith("--") && !temp.equals("")) {
                    sb.append(temp);
                }
            }
            Pattern splitPattern = Pattern.compile(";");
            stmts = splitPattern.split(sb.toString());
        } finally{
            ResourceUtils.closeQuietly(reader);
        }
        return stmts;
    }

    private String[] getDbCreateStatementsFromDb(Connection dbConn) throws SQLException {

        Statement getCreateTablesStmt = null;
        List<String> list = new ArrayList<String>();
        try {
            getCreateTablesStmt = dbConn.createStatement();
            //getCreateTablesStmt.execute("use " + sourceDbName);

            Set<String> tableNames = getTableNames(dbConn);

            //turn off foreign key checks so that tables with constraints can be created before the constraints exists

            list.add("SET FOREIGN_KEY_CHECKS = 0");
            for (String tableName : tableNames) {
                ResultSet createTables = getCreateTablesStmt.executeQuery("show create table " + tableName);
                while (createTables.next()) {
                    String s = createTables.getString(2).replace("\n", "");
                    list.add(s);
                }
            }
        } finally {
//            ResourceUtils.closeQuietly(conn);
            ResourceUtils.closeQuietly(getCreateTablesStmt);
        }

        return list.toArray(new String[list.size()]);
    }

    private boolean testForExistingDb(Connection connection, String dbName) {
        boolean dbExists = false;

        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(SQL_USE_DB + dbName);
            dbExists = true;       //if an exception was not thrown then the db must already exist.
        } catch (SQLException e) {
            if ("42000".equals(e.getSQLState()))      //this means the db didn't exist. In this case, the exception is what we want.
                dbExists = false;
        } finally {
            ResourceUtils.closeQuietly( statement );
        }

        return dbExists;
    }

    private String makeConnectionString(String hostname, int port, String dbName) {
        String urlPattern = SyspropUtil.getString("com.l7tech.config.dburl", DEFAULT_DB_URL);
        return MessageFormat.format( urlPattern, hostname, Integer.toString(port), dbName );
    }

    private Map<String, String[]> buildUpgradeMap(File parentDir) {
        File[] upgradeScripts = parentDir.listFiles(new FilenameFilter() {
                public boolean accept(File file, String s) {
                    return s.toUpperCase().startsWith("UPGRADE") &&
                            s.toUpperCase().endsWith("SQL");
                }
        });

        Pattern upgradePattern = Pattern.compile(UPGRADE_SQL_PATTERN);
        Map<String, String[]> upgradeMap = new HashMap<String, String[]>();

        for (File upgradeScript : upgradeScripts) {
            Matcher matcher = upgradePattern.matcher(upgradeScript.getName());
            if (matcher.matches()) {
                String startVersion = matcher.group(1);
                String destinationVersion = matcher.group(2);
                upgradeMap.put(startVersion, new String[]{destinationVersion, upgradeScript.getAbsolutePath()});
            }
        }
        return upgradeMap;
    }


    private boolean doDbUpgrade(DatabaseConfig databaseConfig, String schemaFilePath, String currentVersion, String dbVersion, DBActionsListener ui) throws IOException {
        boolean isOk;

        if (ui != null) ui.showSuccess("Testing the upgrade on a test database ... " + EOL_CHAR);

        DBActions.DBActionsResult testUpgradeResult = testDbUpgrade(databaseConfig, schemaFilePath, dbVersion, currentVersion, ui);
        if (testUpgradeResult.getStatus() != DB_SUCCESS) {
            String msg = "The database was not upgraded due to the following reasons:\n\n" + testUpgradeResult.getErrorMessage() + "\n\n" +
                    "No changes have been made to the database. Please correct the problem and try again.";
            if (ui != null) ui.showErrorMessage(msg);
            logger.warning(msg);
            isOk = false;
        } else {
            logger.info("Attempting to upgrade the existing database \"" + databaseConfig.getName()+ "\"");
            DBActionsResult upgradeResult = upgradeDbSchema(databaseConfig, schemaFilePath, dbVersion, currentVersion, ui);
            String msg;
            switch (upgradeResult.getStatus()) {
                case DBActions.DB_SUCCESS:
                    logger.info("The database was successfully upgraded");
                    isOk = true;
                    break;
                case DBActions.DB_AUTHORIZATION_FAILURE:
                    msg = "login to the database with the supplied credentials failed, please try again";
                    logger.warning(msg);
                    if (ui != null) ui.showErrorMessage(msg);
                    isOk = false;
                    break;
                case DBActions.DB_UNKNOWNHOST_FAILURE:
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

    private DBActionsResult testDbUpgrade(DatabaseConfig databaseConfig, String schemaFilePath, String dbVersion, String currentVersion, DBActionsListener ui) throws IOException {
        DBActionsResult result = new DBActionsResult();
        DatabaseConfig testDatabaseConfig = new DatabaseConfig( databaseConfig );
        testDatabaseConfig.setName(databaseConfig.getName() + "_testUpgrade");
        try {
            if (ui != null) ui.showSuccess("Creating the test database \"" + testDatabaseConfig.getName() + "\" (without audits)." +
                    EOL_CHAR +
                    "this might take a few minutes" +
                    EOL_CHAR);

            copyDatabase(databaseConfig, testDatabaseConfig, true, ui);
            if (ui != null) ui.showSuccess("The test database was created." + EOL_CHAR);
            if (ui != null) ui.showSuccess("Upgrading the test database. This could take a while" + EOL_CHAR);
            DBActionsResult upgradeResult  = upgradeDbSchema(testDatabaseConfig, schemaFilePath, dbVersion, currentVersion, ui);
            if (upgradeResult.getStatus() != DB_SUCCESS) {
                result.setStatus(DB_CANNOT_UPGRADE);
                result.setErrorMessage(upgradeResult.getErrorMessage());
            } else {
                logger.info(testDatabaseConfig.getName() + " was successfully upgraded.");
            }
        } catch (SQLException e) {
            result.setStatus(determineErrorStatus(e.getSQLState()));
            result.setErrorMessage(ExceptionUtils.getMessage(e));
        } finally {
            //get rid of the temp database
            dropDatabase(testDatabaseConfig, null, true, false, null);
        }

        return result;
    }

    private int determineErrorStatus(String sqlState) {
        if (StringUtils.equals(ERROR_CODE_UNKNOWNDB, sqlState))
            return DB_UNKNOWNDB_FAILURE;
        else if (StringUtils.equals(ERROR_CODE_AUTH_FAILURE, sqlState))
            return DB_AUTHORIZATION_FAILURE;
        else if (StringUtils.equals("08S01", sqlState))
            return DB_UNKNOWNHOST_FAILURE;
        else
            return DB_UNKNOWN_FAILURE;
    }

    public void copyDatabase(DatabaseConfig sourceConfig, DatabaseConfig targetConfig, boolean skipAudits, DBActionsListener ui) throws SQLException {
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

            createDatabase( targetConfig );

            targetConnection = getConnection(targetConfig, false);
            targetConnection.setAutoCommit(false);

            copyDbSchema(sourceConnection, targetConnection);
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

    private void copyDbSchema(Connection sourceConnection, Connection targetConnection ) throws SQLException {
        Statement copyDbStmt = null;
        try {
            String[] createStatements = getDbCreateStatementsFromDb(sourceConnection);
            executeUpdates( targetConnection, createStatements );
        } finally {
            ResourceUtils.closeQuietly(copyDbStmt);
        }
    }

    private void copyDatabaseContents(Connection privSourceConn, final Connection privDestConn, boolean skipAudits) throws SQLException {
        Statement stmt = null;
        try {
            stmt = privDestConn.createStatement();
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            getDbDataStatements(privSourceConn, skipAudits, new StatementUser() {
                public void useStatement(String tableName, List<String> rowData) throws SQLException {
                    int size = rowData.size();
                    StringBuilder sql = new StringBuilder(512);
                    sql.append("insert into ").append(tableName).append(
                            " values (").append(StringUtils.repeat("?, ", size -1)).append('?').append(");");

                    //noinspection JDBCPrepareStatementWithNonConstantString
                    PreparedStatement pStmt = null;
                    try {
                        pStmt = privDestConn.prepareStatement(sql.toString());
                        for (int i = 0; i < size; i++) {
                            pStmt.setString(i+1 , rowData.get(i));
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

    /**
     * Prepare, execute and close a new statement on the specified database connection, using the specified
     * literal SQL which is assumed to contain a query that will produce a ResultSet,
     * and invoking the specified visitor on each row of the resulting ResultSet.
     *
     * @return the number of times the visitor's visit method was called on a result row.  This will be equal
     *         to the number of rows in the result set as long as the visitor never changes the cursor position.
     * @param c the JDBC connection on which to run the query.  Required.
     * @param sql A raw SQL query which will be passed to the database unmodified and without any parameter substitution. Required.
     * @param visitor a visitor to invoke on each row of the result.  If null, the ResultSet will still be cursored through
     *                all the resulting rows to obtain a count, but the contents of each row's data will be ignored.
     * @throws java.sql.SQLException if the statement cannot be prepared or executed, or if the visitor throws SQLException
     */
    public static int query(Connection c, String sql, ResultVisitor visitor) throws SQLException {
        return query(c, sql, null, visitor);
    }

    /**
     * Prepare, execute and close a new statement on the specified database connection, using the specified
     * literal SQL which is assumed to contain a query that will produce a ResultSet,
     * and invoking the specified visitor on each row of the resulting ResultSet.
     *
     * @return the number of times the visitor's visit method was called on a result row.  This will be equal
     *         to the number of rows in the result set as long as the visitor never changes the cursor position.
     * @param c the JDBC connection on which to run the query.  Required.
     * @param sql A raw SQL query which will be passed to the database unmodified. Required.
     *            It may contain parameter placeholders ("?" characters) as long as a non-empty parameterValues array is provided.
     * @param parameterValues values for parameter placeholders in the sql, or null if sql contains no placeholders.
     * @param visitor a visitor to invoke on each row of the result.  If null, the ResultSet will still be cursored through
     *                all the resulting rows to obtain a count, but the contents of each row's data will be ignored.
     * @throws java.sql.SQLException if the statement cannot be prepared or executed, or if the visitor throws SQLException
     */
    public static int query(Connection c, String sql, Object[] parameterValues, ResultVisitor visitor) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = c.prepareStatement(sql);
            setObjects(ps, parameterValues);
            rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                if (visitor != null) visitor.visit(rs);
                count++;
            }
            return count;
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(ps);
        }
    }

    /**
     * Prepare, execute and close a new statement on the specified database connection, using the specified
     * literal SQL, which is assumed to contain a query such as DELETE that will not produce a ResultSet.
     *
     * @param c the JDBC connection on which to run the query.  Required.
     * @param sql A raw SQL query which will be passed to the database unmodified. Required.
     *            It may contain parameter placeholders ("?" characters) as long as a non-empty parameterValues array is provided.
     * @param parameterValues values for parameter placeholders in the sql, or null if sql contains no placeholders.
     * @throws java.sql.SQLException if the statement cannot be prepared or executed
     */
    public static void delete(Connection c, String sql, Object[] parameterValues) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = c.prepareStatement(sql);
            setObjects(ps, parameterValues);
            ps.executeUpdate();
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(ps);
        }
    }

    /**
     * Prepare, execute and close a new INSERT statement on the specified database connection which will
     * add a row to the specified table with the specified column values in order.
     *
     * @param c the JDBC connection on which to run the query.  Required.
     * @param tablename the name of the table to which a row is to be added.  Required.
     * @param columnNames  names of the columns corresponding to the values provided for columnValues, or null
     *                     to assume that the columns exactly match the full schema of the table.
     *                     <p/>
     *                     If columnNames are provided, the generated SQL will resemble
     *                     "insert into tablename (colname1, colname2) values (?,?)"; otherwise, it will resemble
     *                     "insert into tablename values(?,?)".
     *
     * @param columnValues the complete column values for the row, in order.  The number of values provided
     *                   must match the number of columns in the table, and their Java runtime types must be
     *                   mappable by this JDBC driver's {@link java.sql.PreparedStatement#setObject(int, Object)} method
     *                   into the correct database types of each corresponding column.  In particular, care should
     *                   be taken when passing null values as columnValues; see the javadoc for the setObject method
     *                   for details.
     * @throws java.sql.SQLException if the statement cannot be prepared or executed, or if columnNames are provided
     *                               but its size does not match the size of columnValues.
     */
    public static void insert(Connection c, String tablename, String[] columnNames, Object... columnValues) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            boolean first;
            StringBuilder sql = new StringBuilder("insert into " + tablename);
            if (columnNames != null && columnNames.length > 0) {
                if (columnNames.length != columnValues.length)
                    throw new SQLException("insert was given " + columnNames.length + " column names, but "
                                            + columnValues.length + " column values");
                sql.append(" (");
                first = true;
                for (String name : columnNames) {
                    if (!first) sql.append(",");
                    first = false;
                    sql.append(name);
                }
                sql.append(")");
            }
            sql.append(" values(");
            first = true;
            //noinspection UnusedDeclaration
            for (Object columnValue : columnValues) {
                if (!first) sql.append(",");
                first = false;
                sql.append("?");
            }
            sql.append(")");

            ps = c.prepareStatement(sql.toString());
            setObjects(ps, columnValues);
            ps.execute();

        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(ps);
        }
    }

    private static void setObjects(PreparedStatement ps, Object... columnValues) throws SQLException {
        if (columnValues != null)
            for (int i = 0; i < columnValues.length; i++)
                ps.setObject(i + 1, columnValues[i]);
    }

    private interface StatementUser {
        void useStatement(String tableName, List<String> rowData) throws SQLException;
    }

    private void getDbDataStatements(Connection connection, boolean skipAudits, StatementUser statementUser) throws SQLException {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            Set<String> tableNames = getTableNames(connection);

            for (String tableName : tableNames) {
                if (tableName.toLowerCase().startsWith("audit") && skipAudits) continue;

                ResultSet dataRs = stmt.executeQuery("select * from " + tableName);
                ResultSetMetaData meta = dataRs.getMetaData();
                while (dataRs.next()) {
                    List<String> rowData = new ArrayList<String>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        rowData.add(dataRs.getString(i));
                    }
                    statementUser.useStatement(tableName, rowData);
                }
            }
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
    }

    public void setLicenseChecker(LicenseChecker licenseChecker) {
        licChecker = licenseChecker;
    }

    public class WrongDbVersionException extends Exception {
        String dbVersionMessage = null;
        private String dbVersion;

        public WrongDbVersionException(String version, String versionMsg) {
            setVersionInfo(version, versionMsg);
        }

        public WrongDbVersionException(String s, String version, String versionMsg) {
            super(s);
            setVersionInfo(version, versionMsg);
        }

        public WrongDbVersionException(String s, Throwable throwable, String version, String versionMsg) {
            super(s, throwable);
            setVersionInfo(version, versionMsg);
        }

        private void setVersionInfo(String version, String versionMsg) {
            dbVersion = version;
            dbVersionMessage = versionMsg;
        }

        public String getVersionMessage() {
            return dbVersionMessage;
        }

        public String getVersionString() {
            return dbVersion;
        }
    }

    public static class DBActionsResult {
        private int status = DB_ERROR;
        private String errorMessage = null;

        public DBActionsResult() {
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    private static final class DBPermission {
        private final DatabaseConfig databaseConfig;
        private final Set<String> hosts;
        private final boolean isGrant;

        /**
         * Create a new DBPermission
         *
         * @param databaseConfig the DBInformation object that contains the information for the permission to be generated
         * @param hosts the database hosts for the cluster
         * @param isGrant true if this is a grant, false if this is a revocation
         */
        DBPermission(DatabaseConfig databaseConfig, Set<String> hosts, boolean isGrant) {
            this.databaseConfig = databaseConfig;
            this.hosts = hosts;
            this.isGrant = isGrant;
        }

        public String[] getSql() {
            return getStatements();
        }

        public String getPermissionStatement(String hostname) {
            return (isGrant ? SQL_GRANT_ALL : SQL_REVOKE_ALL) +
                    databaseConfig.getName() + ".* " +
                    (isGrant ? "to " : "from ") +
                    databaseConfig.getNodeUsername() + "@'" + hostname +
                    "' identified by '" + databaseConfig.getNodePassword() + "'";
        }

        /**
         * Due to MySQL having grants with empty username for the db host, we need to
         * ensure we add a specific grant for the db user for each db host (since the
         * host is used to check the grants before the user is compared)
         */
        private String[] getStatements() {
            List<String> list = new ArrayList<String>();
            list.add(getPermissionStatement("%"));


            Set<String> allhosts = new HashSet<String>();
            allhosts.add( databaseConfig.getHost() );
            if ( hosts != null ) {
                allhosts.addAll( hosts );
            }

            for ( String host : allhosts ) {
                host = host.trim();

                //grant the ACTUAL hostname, not the localhost one
                try {
                    if ( !host.equals("localhost") &&
                         !host.equals("127.0.0.1") &&
                         !host.equals("localhost.localdomain")) {
                        // add pre-canonicalized
                        list.add(getPermissionStatement(host));

                        // add canonical
                        list.add(getPermissionStatement(InetAddress.getByName(host).getCanonicalHostName()));
                    }
                } catch ( UnknownHostException uhe ) {
                    logger.warning("Could not resolve canonical hostname for '"+host+"'.");
                }
            }

            list.add(getPermissionStatement("localhost"));
            list.add(getPermissionStatement("localhost.localdomain"));
            list.add("FLUSH PRIVILEGES");
            
            return list.toArray(new String[list.size()]);
        }
    }

    class ProgressTimerTask extends TimerTask {
        private final DBActionsListener ui;

        ProgressTimerTask(DBActionsListener ui) {
            this.ui = ui;
        }


        public void run() {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            ui.showSuccess(". ");
        }
    }

    /**
     * Callback used to invoke the visitor on each row of the ResultSet.
     * <p/>
     * We don't just use Callable since we take an arg; we don't use Functions since we need to throw a checked exception.
     */
    public interface ResultVisitor {
        /**
         * Visit a row of the ResultSet.
         * If this method returns normally, it will be assumed that the row was processed successfully.
         *
         * @param rs the result set, already positioned on a row and ready to have columns values read from it.
         *           Never null.  Implementations should generally avoid changing the cursor position.
         * @throws java.sql.SQLException if there is a SQLException while processing the row
         */
        void visit(ResultSet rs) throws SQLException;
    }
}