package com.l7tech.server.config.db;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.*;
import com.l7tech.server.partition.PartitionInformation;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various DB manipulaton and checking methods used by the configuration wizard. Can probably be made more generic to be
 * usefult others as well.
 *
 * User: megery
 */
public class DBActions {
    private static final Logger logger = Logger.getLogger(DBActions.class.getName());
    private final static String MYSQL_CONNECTION_PREFIX = "jdbc:mysql://";

    public static final int DB_SUCCESS = 0;
    public static final int DB_ALREADY_EXISTS = 2;
    public static final int DB_CREATEFILE_MISSING = 3;
    public static final int DB_INCORRECT_VERSION = 4;

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
    public static final String USERNAME_KEY = "Username";
    public static final String PASSWORD_KEY = "Password";

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

    private CheckSSGDatabase ssgDbChecker;
    private OSSpecificFunctions osFunctions;

    private DbVersionChecker[] dbCheckers = new DbVersionChecker[] {
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
    private static final String PROP_HIBERNATE_DRIVER_CLASS = "hibernate.connection.driver_class";
    private static final Object DEFAULT_DB_DRIVER_PROP = "DB_DRIVER";
    private LicenseChecker licChecker;

    //
    // CONSTRUCTOR
    //
    public DBActions() throws ClassNotFoundException {
        init();
    }

    public DBActions(OSSpecificFunctions osf) throws ClassNotFoundException {
        this.osFunctions = osf;
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

    public String checkDbVersion(String hostname, String dbName, String username, String password) {
        Connection conn = null;
        String dbVersion = null;
        try {
            conn = getConnection(hostname, dbName, username, password);
            dbVersion = checkDbVersion(conn, dbName);
        } catch (SQLException e) {
            logger.warning("Error while checking the database version: " + ExceptionUtils.getMessage(e));
        }
        return dbVersion;
    }

    public DBActionsResult createDb(String privUsername, String privPassword, String dbHostname, String dbName, String dbUsername,
                                    String dbPassword, String dbCreateScript, boolean isWindows, boolean overwriteDb) throws IOException {
        DBActionsResult result = new DBActionsResult();

        Connection conn = null;
        Statement stmt = null;
        try {
            DBInformation dbInfo = new DBInformation(dbHostname, dbName, dbUsername, dbPassword, privUsername, privPassword);
            conn = getConnection(dbHostname, ADMIN_DB_NAME, privUsername, privPassword);
            stmt = conn.createStatement();

            if (testForExistingDb(stmt, dbName)) {
                if (!overwriteDb)
                    result.setStatus(DB_ALREADY_EXISTS);
                else {
                    //we should overwrite the db
                    dropDatabase(stmt, dbName, false);
                    makeDatabase(stmt, dbInfo, dbCreateScript, isWindows);
                    result.setStatus(DB_SUCCESS);
                }
            } else {
                makeDatabase(stmt, dbInfo, dbCreateScript, isWindows);
                result.setStatus(DB_SUCCESS);

            }
        } catch (SQLException e) {
            result.setStatus(determineErrorStatus(e.getSQLState()));
            result.setErrorMessage(ExceptionUtils.getMessage(e));

            logger.warning("Could not create database. An exception occurred." + ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly(conn);           
            ResourceUtils.closeQuietly(stmt);
        }

        return result;
    }


    public DBActionsResult upgradeDbSchema(String hostname, String username, String password, String databaseName, String oldVersion,
                                           String newVersion) throws IOException {
        DBActionsResult result = new DBActionsResult();
        File f = new File(osFunctions.getPathToDBCreateFile());

        Map<String, String[]> upgradeMap = buildUpgradeMap(f.getParentFile());

        Connection conn = null;
        Statement stmt = null;

        try {
            conn = getConnection(hostname, databaseName, username, password);
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

                    conn.setAutoCommit(false);
                    for (String statement : statements) {
                        stmt.executeUpdate(statement);
                    }

                    conn.commit();
                    conn.setAutoCommit(true);

                    oldVersion = checkDbVersion(conn, databaseName);
                }
            }
        } catch (SQLException e) {
            result.setStatus(determineErrorStatus(e.getSQLState()));
            result.setErrorMessage(ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly(stmt);
            ResourceUtils.closeQuietly(conn);
        }

        return result;
    }

    public boolean doCreateDb(String pUsername, String pPassword, String hostname, String name, String username, String password, boolean overwriteDb, DBActionsListener ui) {
        //check if the root username is "" or null, or the password is null. Password is allowed to be "", if there isn't a password
        if (StringUtils.isEmpty(pUsername) || pPassword == null) {
            if (ui != null) {
                String defaultUserName = StringUtils.isEmpty(pUsername)?"root":pUsername;
                Map<String, String> creds = ui.getPrivelegedCredentials(
                    "Please enter the credentials for the root database user (needed to create a database)",
                    "Please enter the username for the root database user (needed to create a database): [" + defaultUserName + "] ",
                    "Please enter the password for the root database user: ",
                    defaultUserName);

                if (creds == null) {
                    return false;
                } else {
                    pUsername = creds.get(USERNAME_KEY);
                    pPassword = creds.get(PASSWORD_KEY);
                }
            }
        }

        String errorMsg;
        boolean isOk = false;

        DBActionsResult result = null;
        try {
            logger.info("Attempting to create a new database (" + hostname + "/" + name + ") using privileged user \"" + pUsername + "\"");

            result = createDb(pUsername,
                    pPassword,
                    hostname,
                    name,
                    username,
                    password,
                    osFunctions.getPathToDBCreateFile(),
                    osFunctions.isWindows(),
                    overwriteDb);

            int status = result.getStatus();
            if ( status == DBActions.DB_SUCCESS) {
                isOk = true;
                if (ui != null)
                    ui.showSuccess("Database Successfully Created\n");
            } else {
                switch (status) {
                    case DBActions.DB_UNKNOWNHOST_FAILURE:
                        errorMsg = "Could not connect to the host: \"" + hostname + "\". Please check the hostname and try again.";
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
                        logger.warning("The database named \"" + name + "\" already exists");
                        if (ui != null) {
                            if (ui.getOverwriteConfirmationFromUser(name)) {
                                logger.info("creating new database (overwriting existing one)");
                                logger.warning("The database will be overwritten");
                                isOk = doCreateDb(pUsername, pPassword, hostname, name, username, password, true, ui);
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
            errorMsg = "Could not create the database because there was an error while reading the file \"" + osFunctions.getPathToDBCreateFile() + "\"." +
                    " The error was: " + ExceptionUtils.getMessage(e);
            logger.warning(errorMsg);
            if (ui != null) ui.showErrorMessage(errorMsg);
            isOk = false;
        }

        return isOk;
    }

    public boolean doExistingDb(String dbName, String hostname, String username, String password, String privUsername, String privPassword, String currentVersion, DBActionsListener ui) {
        String errorMsg;
        boolean isOk;

        logger.info("Attempting to connect to an existing database (" + hostname + "/" + dbName + ")" + "using username/password \"" + username + "/" + password + "\"");

        DBInformation dbInfo = new DBInformation(hostname, dbName, username, password, privUsername, privPassword);

        DBActions.DBActionsResult status = checkExistingDb(dbInfo);
        if (status.getStatus() == DBActions.DB_SUCCESS) {
            logger.info(CONNECTION_SUCCESSFUL_MSG);

            if (licChecker != null && !checkLicense(ui, currentVersion, dbInfo))
                return false;

            logger.info("Now Checking database version.");
            String dbVersion = checkDbVersion(hostname, dbName, username, password);
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
                            Map<String, String> creds = null;
                            if (StringUtils.isEmpty(privUsername) || StringUtils.isEmpty(privPassword)) {
                                if (ui != null) creds = ui.getPrivelegedCredentials(
                                        "Please enter the credentials for the root database user (needed to upgrade the database)",
                                        "Please enter the username for the root database user (needed to upgrade the database): [root]",
                                        "Please enter the password for root database user (needed to upgrade the database): ", "root");

                                else return false;
                            }
                            if (creds == null) return false;
                            privUsername = creds.get(DBActions.USERNAME_KEY);
                            dbInfo.setPrivUsername(privUsername);

                            privPassword = creds.get(DBActions.PASSWORD_KEY);
                            dbInfo.setPrivPassword(privPassword);

                            isOk = doDbUpgrade(dbInfo, currentVersion, dbVersion, ui);
                            if (isOk && ui != null) ui.showSuccess("Database Successfully Upgraded\n");
                        } catch (IOException e) {
                            errorMsg = "There was an error while attempting to upgrade the database";
                            logger.severe(errorMsg);
                            logger.severe(ExceptionUtils.getMessage(e));
                            if (ui != null) ui.showErrorMessage(errorMsg);
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

    private boolean checkLicense(DBActionsListener ui, String currentVersion, DBInformation dbInfo) {
        logger.info("Now Checking SSG License validity");
        Connection conn = null;
        try {
            conn = getConnection(dbInfo);
        } catch (SQLException e) {
            logger.info("Cannot check license. Could not get a connection to the database");
            return false;
        }
        try {
            licChecker.checkLicense(conn, currentVersion, BuildInfo.getProductName(), BuildInfo.getProductVersionMajor(), BuildInfo.getProductVersionMinor());
            logger.info("License is valid and will work with this version (" + currentVersion + ").");
        } catch (InvalidLicenseException e) {
            String message = ExceptionUtils.getMessage(e);
            logger.warning(message);
            if (ui != null) {
                if (!ui.getGenericUserConfirmation(message + "\nDo you wish to continue?")) {
                    ui.showErrorMessage(message);
                    return false;
                }
            }
        }
        return true;
    }

    public Connection getConnection(DBInformation dbInfo) throws SQLException {
        return getConnection(dbInfo.getHostname(), dbInfo.getDbName(), dbInfo.getUsername(),dbInfo.getPassword());
    }

    public Connection getConnection(String hostname, String dbName, String username, String password) throws SQLException {
        return DriverManager.getConnection(makeConnectionString(hostname, dbName), username, password);
    }

    public void dropDatabase(DBInformation dbInfo, boolean isInfo, boolean doRevoke, DBActionsListener ui) {
        String pUsername = dbInfo.getPrivUsername();
        String pPassword = dbInfo.getPrivPassword();
        if (StringUtils.isEmpty(pUsername) || pPassword == null) {
            if (ui != null) {
                String defaultUserName = StringUtils.isEmpty(pUsername)?"root":pUsername;
                Map<String, String> creds = ui.getPrivelegedCredentials(
                    "Please enter the credentials for the root database user (needed to drop the database)",
                    "Please enter the username for the root database user: [" + defaultUserName + "] ",
                    "Please enter the password for the root database user: ",
                    defaultUserName);

                if (creds == null) {
                    return;
                } else {
                    pUsername = creds.get(USERNAME_KEY);
                    pPassword = creds.get(PASSWORD_KEY);
                }
            }
        }

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection(dbInfo.getHostname(), ADMIN_DB_NAME, pUsername, pPassword);
        } catch (SQLException connectException) {
            logger.severe("Failure while dropping the database. Could not connect to the admin database on the server (" + ADMIN_DB_NAME + ") as user " + pUsername + "\". Please drop the database manually.");
        }

        if (conn != null) {
            try {
                boolean dropSucceeded = false;
                try {
                    stmt = conn.createStatement();
                    dropDatabase(stmt, dbInfo.getDbName(), isInfo);
                    dropSucceeded = true;
                } catch (SQLException e) {
                    logger.severe("Failure while dropping the database: " + ExceptionUtils.getMessage(e));
                }

                if (dropSucceeded && doRevoke) {
                    String [] revokeStatements = getRevokeStatements(dbInfo, osFunctions.isWindows());
                    for (String revokeStatement : revokeStatements) {
                        try {
                            stmt.executeUpdate(revokeStatement);
                        } catch (SQLException e) {
                            //don't want to halt on errors here
                        }
                    }
                }
            } finally {
                ResourceUtils.closeQuietly(stmt);
                ResourceUtils.closeQuietly(conn);
            }
        }
    } 

//
// PRIVATE METHODS
//

    private void dropDatabase(Statement stmt, String dbName, boolean isInfo) throws SQLException {
        stmt.executeUpdate(SQL_DROP_DB + dbName);

        if (isInfo)
            logger.info("dropping database \"" + dbName + "\"");
        else
            logger.warning("dropping database \"" + dbName + "\"");
    }

    private DBActionsResult checkExistingDb(DBInformation dbInfo) {

        DBActionsResult result = new DBActionsResult();
        String connectionString = makeConnectionString(dbInfo.getHostname(), dbInfo.getDbName());
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(connectionString, dbInfo.getUsername(), dbInfo.getPassword());
            result.setStatus(DB_SUCCESS);
        } catch (SQLException e) {
            String message = ExceptionUtils.getMessage(e);
            result.setStatus(determineErrorStatus(e.getSQLState()));
            result.setErrorMessage(message);

            logger.warning("Could not login to the database using " + dbInfo.getUsername() + ":" + dbInfo.getPassword() + "@" + dbInfo.getHostname() + "/" + dbInfo.getDbName());
            logger.warning(message);
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
        return result;
    }

    private void init() throws ClassNotFoundException {
        if (osFunctions == null) osFunctions = OSDetector.getOSSpecificFunctions(PartitionInformation.DEFAULT_PARTITION_NAME);
        initDriver();
        
        ssgDbChecker = new CheckSSGDatabase();
        //always sort the dbCheckers in reverse in case someone has added one out of sequence so things still work properly
        if (!hasCheckForCurrentVersion(dbCheckers)) {
            DbVersionChecker[] checkers = new DbVersionChecker[dbCheckers.length+1];
            System.arraycopy(dbCheckers, 0, checkers, 0, dbCheckers.length);
            checkers[dbCheckers.length] =  new DbVersionBeHappyChecker();
            dbCheckers = checkers;
        }

        Arrays.sort(dbCheckers, Collections.reverseOrder());
    }

    private void initDriver() throws ClassNotFoundException {
        Properties dbProps = new Properties();
        InputStream is = null;
        //try to read the database driver from 

        File templatePartitionDir = new File(osFunctions.getPartitionBase() + "partitiontemplate_");
        File oldConfigLocation = new File(osFunctions.getSsgInstallRoot() + "etc/conf");

        try {
            is = new FileInputStream(new File(templatePartitionDir, "hibernate.properties")) ;
        } catch (FileNotFoundException e) {
            //couldn't find the configuration for the default partition so lets try the old location
            try {
                is = new FileInputStream(new File(oldConfigLocation, "hibernate.properties"));
            } catch (FileNotFoundException e1) {
                throw new RuntimeException(
                        "could not load the database configuration file. Tried: " +
                                oldConfigLocation.getAbsolutePath() + " and " + templatePartitionDir.getAbsolutePath());
            }
        }

        try {
            dbProps.load(is);
            String driverName = dbProps.getProperty(PROP_HIBERNATE_DRIVER_CLASS);
            if (StringUtils.isEmpty(driverName))
                throw new RuntimeException("Could not determine database driver name ");
            if (driverName.equals(DEFAULT_DB_DRIVER_PROP))
                throw new RuntimeException("Could not determine database driver name [found " + DEFAULT_DB_DRIVER_PROP + "]");

            //instantiate the driver class
            Class.forName(JDBC_DRIVER_NAME);
        } catch (IOException e) {
            logger.severe("Error while reading the database configuration file: " + ExceptionUtils.getMessage(e));
            throw new RuntimeException(
                        "could not load the database configuration file. Tried: " +
                                oldConfigLocation.getAbsolutePath() + " and " + templatePartitionDir.getAbsolutePath());
        } finally {
            ResourceUtils.closeQuietly(is);
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
        ResultSet rs = null;
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
            boolean versionFound = false;
            do {
                DbVersionChecker checker = dbCheckers[dbCheckIndex++];
                if (versionFound = checker.doCheck(tableData)) {
                     version = checker.getVersion();
                }

            } while(!versionFound && dbCheckIndex < dbCheckers.length);
         }
         return version;
     }

    //checks the database version heuristically by looking for table names, columns etc. known to have been introduced
    //in particular versions. The checks are, for the most part, self contained and the code is designed to be extensible.
    private String checkDbVersion(Connection conn, String dbName) throws SQLException {
        Map<String, Set<String>> tableData = collectMetaInfo(conn, dbName);
        //now we have a hashtable of tables and their columns
        return getDbVersion(tableData);
    }

    private Set<String> getTableNames(Connection conn, String sourceDbName) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute("use " + sourceDbName);
            Set<String> tables = new HashSet<String>();

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

    private Map<String, Set<String>> collectMetaInfo(Connection conn, String dbName) throws SQLException {
        Map<String, Set<String>> tableData = new HashMap<String, Set<String>>();

        Set<String> tables = getTableNames(conn, dbName);
        for (String tableName : tables) {
            Set<String> columns = getTableColumns(tableName, conn.getMetaData());
            if (columns != null)
                tableData.put(tableName.toLowerCase(), columns);
        }
        return tableData;
    }

    private void makeDatabase(Statement stmt, DBInformation dbInfo, String dbCreateScript, boolean isWindows) throws SQLException, IOException {
        stmt.getConnection().setAutoCommit(false); //start transaction
        String newDbName = dbInfo.getDbName();

        logger.info("creating database \"" + newDbName +"\"");

        createDatabase(stmt.getConnection(), newDbName);

        // fla - maybe i just want to create the database without adding tables and rows yet
        if (dbCreateScript != null) {
            executeUpdates(getCreateDbStatementsFromFile(dbCreateScript),
                           stmt, "Creating schema for " + newDbName + " database", newDbName);
        } else {
            logger.info("Skipping creation of tables and rows");
        }
        executeUpdates(
                getGrantStatements(dbInfo, isWindows),
                stmt,
                "Creating user \"" + dbInfo.getUsername() + "\" and performing grants on " + newDbName + " database", newDbName);

        stmt.getConnection().commit();     //finish transaction
        stmt.getConnection().setAutoCommit(true);
    }

    public String[] getGrantStatements(DBInformation dbInfo, boolean isWindows) {
        return getPermissionChangeStatements(dbInfo,  isWindows, true);
    }

    private String[] getRevokeStatements(DBInformation dbInfo, boolean isWindows) {
        return getPermissionChangeStatements(dbInfo,  isWindows, false);
    }

    private void createDatabase(Connection dbConnection, String dbName) throws SQLException {
        Statement stmt = null;
        try {
            stmt = dbConnection.createStatement();
            stmt.executeUpdate(SQL_CREATE_DB + dbName);
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }

    }

    private void executeUpdates(String[] sqlStatements, Statement stmt, String logMessage, String whichDb) throws SQLException {
        if (sqlStatements != null) {
            if (StringUtils.isNotEmpty(logMessage))
                logger.info(logMessage);

            stmt.execute("use " + whichDb);
            for (String sqlStmt : sqlStatements) {
                stmt.executeUpdate(sqlStmt);
            }
        }
    }

    private String[] getPermissionChangeStatements(DBInformation dbInfo, boolean isWindows, boolean doGrants) {
        return new DBPermission(dbInfo, isWindows, doGrants).getStatements();
    }

    private String[] getCreateDbStatementsFromFile(String dbCreateScript) throws IOException {
        String []  stmts = null;
        BufferedReader reader = null;
        try {
            StringBuffer sb = new StringBuffer();

            reader = new BufferedReader(new FileReader(dbCreateScript));
            String temp = null;
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

    private String[] getDbCreateStatementsFromDb(Connection dbConn, String sourceDbName) throws SQLException {

Statement getCreateTablesStmt = null;
        List<String> list = new ArrayList<String>();
        try {
            getCreateTablesStmt = dbConn.createStatement();
            getCreateTablesStmt.execute("use " + sourceDbName);

            Set<String> tableNames = getTableNames(dbConn, sourceDbName);

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

    private boolean testForExistingDb(Statement stmt, String dbName) {
        boolean dbExists = false;
        try {
            stmt.execute(SQL_USE_DB + dbName);
            dbExists = true;       //if an exception was not thrown then the db must already exist.
        } catch (SQLException e) {
            if ("42000".equals(e.getSQLState()))      //this means the db didn't exist. In this case, the exception is what we want.
                dbExists = false;
        }
        return dbExists;
    }

    private String makeConnectionString(String hostname, String dbName) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(MYSQL_CONNECTION_PREFIX).append(hostname).append("/").append(dbName);
        return buffer.toString();
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


    private boolean doDbUpgrade(DBInformation dbInfo, String currentVersion, String dbVersion, DBActionsListener ui) throws IOException {
        boolean isOk = false;

        DBActions.DBActionsResult testUpgradeResult = testDbUpgrade(dbInfo, dbVersion, currentVersion);
        if (testUpgradeResult.getStatus() != DB_SUCCESS) {
            String msg = "The database was not upgraded due to the following reasons:\n\n" + testUpgradeResult.getErrorMessage() + "\n\n" +
                    "No changes have been made to the database. Please correct the problem and try again.";
            if (ui != null) ui.showErrorMessage(msg);
            logger.warning(msg);
            isOk = false;
        } else {
            logger.info("Attempting to upgrade the existing database \"" + dbInfo.getDbName()+ "\"");
            DBActionsResult upgradeResult = upgradeDbSchema(dbInfo.getHostname(), dbInfo.getPrivUsername(), dbInfo.getPrivPassword(), dbInfo.getDbName(), dbVersion, currentVersion);
            String msg;
            switch (upgradeResult.getStatus()) {
                case DBActions.DB_SUCCESS:
                    logger.info("Database successfully upgraded");
                    isOk = true;
                    break;
                case DBActions.DB_AUTHORIZATION_FAILURE:
                    msg = "login to the database with the supplied credentials failed, please try again";
                    logger.warning(msg);
                    if (ui != null) ui.showErrorMessage(msg);
                    isOk = false;
                    break;
                case DBActions.DB_UNKNOWNHOST_FAILURE:
                    msg = "Could not connect to the host: \"" + dbInfo.getHostname() + "\". Please check the hostname and try again.";
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

    private DBActionsResult testDbUpgrade(DBInformation dbInfo, String dbVersion, String currentVersion) throws IOException {
        DBActionsResult result = new DBActionsResult();
        String testDbName = dbInfo.getDbName() + "_testUpgrade";
        try {
            copyDatabase(dbInfo, testDbName);
            DBActionsResult upgradeResult  = upgradeDbSchema(dbInfo.getHostname(), dbInfo.getPrivUsername(), dbInfo.getPrivPassword(), testDbName, dbVersion, currentVersion);
            if (upgradeResult.getStatus() != DB_SUCCESS) {
                result.setStatus(DB_CANNOT_UPGRADE);
                result.setErrorMessage(upgradeResult.getErrorMessage());
            } else {
                logger.info(testDbName + " was successfully upgraded.");
            }
        } catch (SQLException e) {
            result.setStatus(determineErrorStatus(e.getSQLState()));
            result.setErrorMessage(ExceptionUtils.getMessage(e));
        } finally {
            DBInformation testDbInfo = new DBInformation(dbInfo.getHostname(), testDbName, dbInfo.getUsername(), dbInfo.getPassword(), dbInfo.getPrivUsername(), dbInfo.getPrivPassword());
            //get rid of the temp database
            dropDatabase(testDbInfo, true, false, null);
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

    public void copyDatabase(DBInformation sourceDbInfo, String testDbName) throws SQLException {
        Connection privilegedConnection = null;
        try {
            privilegedConnection = getConnection(sourceDbInfo.getHostname(), sourceDbInfo.getDbName(), sourceDbInfo.getPrivUsername(), sourceDbInfo.getPrivPassword());
            String sourceDbName = sourceDbInfo.getDbName();

            privilegedConnection.setAutoCommit(false);
            copyDbSchema(privilegedConnection, sourceDbName, testDbName);
            copyDatabaseContents(privilegedConnection, sourceDbName, testDbName);
            privilegedConnection.commit();
            privilegedConnection.setAutoCommit(true);

        } finally {
            ResourceUtils.closeQuietly(privilegedConnection);
        }
    }

    private void copyDbSchema(Connection privilegedConnection, String sourceDbName, String destinationDbName) throws SQLException {
        Statement copyDbStmt = null;
        try {
            String[] createStatements = getDbCreateStatementsFromDb(privilegedConnection, sourceDbName);

            createDatabase(privilegedConnection, destinationDbName);
            copyDbStmt = privilegedConnection.createStatement();
            executeUpdates(createStatements, copyDbStmt, "Creating a copy of " + sourceDbName + " to test the upgrade process.", destinationDbName);
        } finally {
            ResourceUtils.closeQuietly(copyDbStmt);
        }
    }

    private void copyDatabaseContents(Connection privilegedConnection, String sourceDbName, String destinationDbName) throws SQLException {
        Statement stmt = null;
        PreparedStatement pStmt = null;
        try {
            Map<String, List<String>> dbData = getDbDataStatements(privilegedConnection, sourceDbName);
            stmt = privilegedConnection.createStatement();
            stmt.execute("use " + destinationDbName);
            for (Map.Entry<String, List<String>> entry : dbData.entrySet()) {
                String tableName = entry.getKey();
                //don't copy audits, takes too long
                if (!"audit_main".equalsIgnoreCase(tableName)) {
                    List<String> values = entry.getValue();
                    int size = values.size();
                    StringBuilder sql = new StringBuilder();
                    sql.append("insert into ").append(entry.getKey()).append(
                            " values (").append(StringUtils.repeat("?, ", size -1)).append("?").append(");");

                    pStmt = privilegedConnection.prepareStatement(sql.toString());
                    for (int i = 0; i < size; i++) {
                        pStmt.setString(i+1 , values.get(i));
                    };
                    pStmt.addBatch();
                    pStmt.executeUpdate();
                }
            }
        } finally {
            ResourceUtils.closeQuietly(stmt);
            ResourceUtils.closeQuietly(pStmt);
        }
    }

    private Map<String, List<String>> getDbDataStatements(Connection connection, String sourceDbName) throws SQLException {
        Statement stmt = null;
        Map<String, List<String>> tableData = new LinkedHashMap<String, List<String>>();
        try {
            stmt = connection.createStatement();
            Set<String> tableNames = getTableNames(connection, sourceDbName);

            stmt.execute("use " + sourceDbName);
            for (String tableName : tableNames) {
                ResultSet dataRs = stmt.executeQuery("select * from " + tableName);
                ResultSetMetaData meta = dataRs.getMetaData();
                while (dataRs.next()) {
                    List<String> rowData = new ArrayList<String>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        rowData.add(dataRs.getString(i));
                    }
                    tableData.put(tableName, rowData);
                }
            }
            return tableData;
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
    }

    public void setLicenseChecker(LicenseChecker licenseChecker) {
        licChecker = licenseChecker;
    }

    public static String getNonLocalHostame(String dbHostname) {
        String realHostname = SharedWizardInfo.getInstance().getRealHostname();

        if (dbHostname.equalsIgnoreCase("localhost") || dbHostname.equalsIgnoreCase("127.0.0.1")) {
            return realHostname;
        }

        if (dbHostname.contains(",")) {
            String[] hosts = dbHostname.split(",");
            String returnMe = "";
            for (int i = 0; i < hosts.length; i++) {
                String host = hosts[i];
                hosts[i]= getNonLocalHostame(host);
                returnMe += ((i == 0)?"":",") + hosts[i];
            }
            return returnMe;
        }

        return dbHostname;

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
        private int status = 0;
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

    private class DBPermission {
        private DBInformation dbInfo;
        private boolean isWindows;
        private boolean isGrant;

        /**
         * Create a new DBPermission
         * @param dbInfo the DBInformation object that contains the information for the permission to be generated
         * @param isWindows true if the database resides on a windows machine, false otherwise
         * @param isGrant true if this is a grant, false if this is a revocation
         */
        DBPermission(DBInformation dbInfo, boolean isWindows, boolean isGrant) {
            this.dbInfo = dbInfo;
            this.isWindows = isWindows;
            this.isGrant = isGrant;
        }

        public String[] getSql() {
            return getStatements();
        }

        public String getPermissionStatement(String hostname) {
            return new String((isGrant?SQL_GRANT_ALL:SQL_REVOKE_ALL) + dbInfo.getDbName() + ".* " + (isGrant?"to ":"from ") + dbInfo.getUsername() + "@'" + hostname + "' identified by '" + dbInfo.getPassword() + "'");
        }

        private String[] getStatements() {
            List<String> list = new ArrayList<String>();
            list.add(getPermissionStatement("%"));

            boolean usesLocalhost = false;

            String dbHostnameString = dbInfo.getHostname();
            //if there are more than one hostname, grant each one separately
            if (dbHostnameString.contains(",")) {
                String[] hosts = dbHostnameString.split(",");
                for (String host : hosts) {
                    host = host.trim();
                    if (host.equalsIgnoreCase("localhost") || host.equalsIgnoreCase("127.0.0.1") )
                        usesLocalhost = true;

                    //grant the ACTUAL hostname, not the localhost one
                    list.add(getPermissionStatement(DBActions.getNonLocalHostame(host)));
                }
            } else {
                dbHostnameString = dbHostnameString.trim();
                if (dbHostnameString.equalsIgnoreCase("localhost") || dbHostnameString.equalsIgnoreCase("127.0.0.1") )
                    usesLocalhost = true;

                list.add(getPermissionStatement(DBActions.getNonLocalHostame(dbHostnameString)));
            }

            //if localhost was used, then grant that too
            if (usesLocalhost) {
                list.add(getPermissionStatement("localhost"));
                if (!isWindows) {
                    list.add(getPermissionStatement("localhost.localdomain"));
                }
            } else {
                //if this is a revoke, we should still add in the localhost stuff
                if (!isGrant) {
                    list.add(getPermissionStatement("localhost"));
                    list.add(getPermissionStatement("localhost.localdomain"));
                }
            }
            list.add(new String("FLUSH PRIVILEGES"));
            return list.toArray(new String[list.size()]);
        }
    }
}