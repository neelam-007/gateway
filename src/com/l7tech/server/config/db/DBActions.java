package com.l7tech.server.config.db;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.util.Background;
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
        } finally {
            ResourceUtils.closeQuietly(conn);
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


    public DBActionsResult upgradeDbSchema(String hostname,
                                           String username,
                                           String password,
                                           String databaseName,
                                           String oldVersion,
                                           String newVersion,
                                           final DBActionsListener ui) throws IOException {
        DBActionsResult result = new DBActionsResult();
        File f = new File(osFunctions.getPathToDBCreateFile());

        Map<String, String[]> upgradeMap = buildUpgradeMap(f.getParentFile());

        Connection conn = null;
        Statement stmt = null;

        TimerTask spammer = null;
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

                    spammer = new ProgressTimerTask(ui);

                    if (ui != null) {
                        ui.showSuccess("Upgrading \"" + databaseName + "\" from " + oldVersion + "->" + upgradeInfo[0] + Utilities.EOL_CHAR) ;
                        ui.showSuccess("Please note that if you have a lot of audits, this may take a long time." + Utilities.EOL_CHAR) ;
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
                    if (ui != null) ui.showSuccess("completed" + Utilities.EOL_CHAR);


                    oldVersion = checkDbVersion(conn, databaseName);
                }
            }
            if (ui != null) ui.showSuccess(databaseName + " was upgraded successfully." + Utilities.EOL_CHAR);

        } catch (SQLException e) {
            result.setStatus(determineErrorStatus(e.getSQLState()));
            result.setErrorMessage(ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly(stmt);
            ResourceUtils.closeQuietly(conn);
            if (spammer != null) {
                Background.cancel(spammer);
                spammer = null;
            }
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

    private String hidepass(String password) {
        // We will only indicate whether or not the password is empty here.
        // To avoid leaking information about the length we won't just mask the characters;
        // to avoid misleadingly implying an incorrect length, we won't just write asterixes
        return password == null || password.length() < 1 ? "<empty>" : "<not shown>";
    }

    public boolean doExistingDb(String dbName, String hostname, String username, String password, String privUsername, String privPassword, String currentVersion, DBActionsListener ui) {
        String errorMsg;
        boolean isOk;

        logger.info("Attempting to connect to an existing database (" + hostname + "/" + dbName + ")" + "using username/password " + username + "/" + hidepass(password));


        if (hostname.equalsIgnoreCase(SharedWizardInfo.getInstance().getRealHostname())) {
            hostname = "localhost";
        }
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
                            if (isOk && ui != null) ui.showSuccess("The database was successfully upgraded\n");
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
            try {
                conn = getConnection(dbInfo);
            } catch (SQLException e) {
                logger.info("Cannot check the license. Could not get a connection to the database");
                return false;
            }
            try {
                licChecker.checkLicense(conn, currentVersion, BuildInfo.getProductName(), BuildInfo.getProductVersionMajor(), BuildInfo.getProductVersionMinor());
                logger.info("The License is valid and will work with this version (" + currentVersion + ").");
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
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
        return true;
    }

    public Connection getConnection(DBInformation dbInfo) throws SQLException {
        return getConnection(dbInfo.getHostname(), dbInfo.getDbName(), dbInfo.getUsername(),dbInfo.getPassword());
    }

    public Connection getConnection(String hostname, String dbName, String username, String password) throws SQLException {
        return DriverManager.getConnection(makeConnectionString(hostname, dbName), username, password);
    }

    public boolean dropDatabase(DBInformation dbInfo, boolean isInfo, boolean doRevoke, DBActionsListener ui) {
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
                    return false;
                } else {
                    pUsername = creds.get(USERNAME_KEY);
                    pPassword = creds.get(PASSWORD_KEY);
                }
            }
        }

        boolean allIsWell = false;
        Connection conn = null;
        Statement stmt = null;
        try {
            String hostname = dbInfo.getHostname();
            if (hostname.equalsIgnoreCase(SharedWizardInfo.getInstance().getRealHostname())) {
                hostname = "localhost";
            }
            conn = getConnection(hostname, ADMIN_DB_NAME, pUsername, pPassword);
            try {
                stmt = conn.createStatement();
                dropDatabase(stmt, dbInfo.getDbName(), isInfo);
                allIsWell = true;
            } catch (SQLException e) {
                logger.severe("Failure while dropping the database: " + ExceptionUtils.getMessage(e));
                allIsWell = false;
            }

            if (allIsWell) {
                if (doRevoke) {
                    String [] revokeStatements = getRevokeStatements(dbInfo, osFunctions.isWindows());
                    for (String revokeStatement : revokeStatements) {
                        try {
                            stmt.executeUpdate(revokeStatement);
                        } catch (SQLException e) {
                            logger.info("While revoking a permission for user " + dbInfo.getUsername() + " an error occurred. The privilege was not found.");
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
        Arrays.sort(dbCheckers, Collections.reverseOrder());

        if (!hasCheckForCurrentVersion(dbCheckers)) {
            DbVersionChecker[] checkers = new DbVersionChecker[dbCheckers.length+1];
            System.arraycopy(dbCheckers, 0, checkers, 1, dbCheckers.length);
            checkers[0] =  new DbVersionBeHappyChecker(dbCheckers.length > 0 ? dbCheckers[0] : null);
            dbCheckers = checkers;
        }
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

        if (ui != null) ui.showSuccess("Testing the upgrade on a test database ... " + Utilities.EOL_CHAR);

        DBActions.DBActionsResult testUpgradeResult = testDbUpgrade(dbInfo, dbVersion, currentVersion, ui);
        if (testUpgradeResult.getStatus() != DB_SUCCESS) {
            String msg = "The database was not upgraded due to the following reasons:\n\n" + testUpgradeResult.getErrorMessage() + "\n\n" +
                    "No changes have been made to the database. Please correct the problem and try again.";
            if (ui != null) ui.showErrorMessage(msg);
            logger.warning(msg);
            isOk = false;
        } else {
            logger.info("Attempting to upgrade the existing database \"" + dbInfo.getDbName()+ "\"");
            DBActionsResult upgradeResult = upgradeDbSchema(dbInfo.getHostname(), dbInfo.getPrivUsername(), dbInfo.getPrivPassword(), dbInfo.getDbName(), dbVersion, currentVersion,ui);
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

    private DBActionsResult testDbUpgrade(DBInformation dbInfo, String dbVersion, String currentVersion, DBActionsListener ui) throws IOException {
        DBActionsResult result = new DBActionsResult();
        String testDbName = dbInfo.getDbName() + "_testUpgrade";
        try {
            if (ui != null) ui.showSuccess("Creating the test database \"" + testDbName + "\" (without audits)." +
                    Utilities.EOL_CHAR +
                    "this might take a few minutes" +
                    Utilities.EOL_CHAR);

            copyDatabase(dbInfo, testDbName, true, ui);
            if (ui != null) ui.showSuccess("The test database was created." + Utilities.EOL_CHAR);
            if (ui != null) ui.showSuccess("Upgrading the test database. This could take a while" + Utilities.EOL_CHAR);
            DBActionsResult upgradeResult  = upgradeDbSchema(dbInfo.getHostname(), dbInfo.getPrivUsername(), dbInfo.getPrivPassword(), testDbName, dbVersion, currentVersion, ui);
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

    public void copyDatabase(DBInformation sourceDbInfo, String testDbName, boolean skipAudits, DBActionsListener ui) throws SQLException {
        Connection privilegedConnection = null;
        Connection privilegedDestConnection = null;
        TimerTask spammer = null;
        try {
            privilegedConnection = getConnection(sourceDbInfo.getHostname(), sourceDbInfo.getDbName(), sourceDbInfo.getPrivUsername(), sourceDbInfo.getPrivPassword());
            String sourceDbName = sourceDbInfo.getDbName();

            privilegedConnection.setAutoCommit(false);
            if (ui != null) {
                spammer = new ProgressTimerTask(ui);
                Background.scheduleRepeated(spammer, 2000, 2000);
            }
            copyDbSchema(privilegedConnection, sourceDbName, testDbName);
            privilegedConnection.commit();

            privilegedDestConnection = getConnection(sourceDbInfo.getHostname(), testDbName, sourceDbInfo.getPrivUsername(), sourceDbInfo.getPrivPassword());
            privilegedDestConnection.setAutoCommit(false);

            copyDatabaseContents(privilegedConnection, privilegedDestConnection, sourceDbName, testDbName, skipAudits);
            privilegedConnection.commit();
            privilegedDestConnection.commit();
            if (spammer != null) {
                Background.cancel(spammer);
                spammer = null;
            }
            privilegedConnection.setAutoCommit(true);
            privilegedDestConnection.setAutoCommit(true);
        } finally {
            ResourceUtils.closeQuietly(privilegedConnection);
            ResourceUtils.closeQuietly(privilegedDestConnection);
            if (spammer != null) {
                Background.cancel(spammer);
                spammer = null;
            }
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
            copyDbStmt = null;
        }
    }

    private void copyDatabaseContents(Connection privSourceConn, final Connection privDestConn, String sourceDbName, String destinationDbName, boolean skipAudits) throws SQLException {
        Statement stmt = null;
        final PreparedStatement[] pStmt = new PreparedStatement[]{null};
        try {
            stmt = privDestConn.createStatement();
            //noinspection JDBCExecuteWithNonConstantString
            stmt.execute("use " + destinationDbName);
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            getDbDataStatements(privSourceConn, sourceDbName, skipAudits, new StatementUser() {
                public void useStatement(String tableName, List<String> rowData) throws SQLException {
                    //don't copy audits, takes too long
                    if ("audit_main".equalsIgnoreCase(tableName))
                        return;

                    int size = rowData.size();
                    StringBuilder sql = new StringBuilder(512);
                    sql.append("insert into ").append(tableName).append(
                            " values (").append(StringUtils.repeat("?, ", size -1)).append('?').append(");");

                    //noinspection JDBCPrepareStatementWithNonConstantString
                    pStmt[0] = privDestConn.prepareStatement(sql.toString());
                    for (int i = 0; i < size; i++) {
                        pStmt[0].setString(i+1 , rowData.get(i));
                    }
                    pStmt[0].addBatch();
                    pStmt[0].executeUpdate();
                    pStmt[0].close();
                    pStmt[0] = null;
                }
            });
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        } finally {
            ResourceUtils.closeQuietly(stmt);
            stmt = null;

            ResourceUtils.closeQuietly(pStmt[0]);
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

    private void getDbDataStatements(Connection connection, String sourceDbName, boolean skipAudits, StatementUser statementUser) throws SQLException {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            Set<String> tableNames = getTableNames(connection, sourceDbName);

            stmt.execute("use " + sourceDbName);
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
            return (isGrant ? SQL_GRANT_ALL : SQL_REVOKE_ALL) +
                    dbInfo.getDbName() + ".* " +
                    (isGrant ? "to " : "from ") +
                    dbInfo.getUsername() + "@'" + hostname +
                    "' identified by '" + dbInfo.getPassword() + "'";
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