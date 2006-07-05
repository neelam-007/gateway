package com.l7tech.server.config.db;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.OSDetector;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

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
    private static final String SQL_DROP_DB = "drop database ";

    private static final String GENERIC_DBCONNECT_ERROR_MSG = "There was an error while attempting to connect to the database. Please try again";

    private CheckSSGDatabase ssgDbChecker;
    private OSSpecificFunctions osFunctions;

    private DbVersionChecker[] dbCheckers = new DbVersionChecker[] {
        new DbVersion36Checker(),
        new DbVersion35Checker(),
        new DbVersion34Checker(),
        new DbVersion33Checker(),
        new DbVersion3132Checker(),
    };
    private static final String UPGRADE_SQL_PATTERN = "^upgrade_(.*)-(.*).sql$";

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

    //
    // CONSTRUCTOR
    //
    public DBActions() throws ClassNotFoundException {
        init();
    }


    //
    // PUBLIC METHODS
    //

    public DBActionsResult checkExistingDb(String dbHostname, String dbName, String dbUsername, String dbPassword) {
        DBActionsResult result = new DBActionsResult();
        String connectionString = makeConnectionString(dbHostname, dbName);
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(connectionString, dbUsername, dbPassword);
            result.setStatus(DB_SUCCESS);
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if (sqlState != null) {
                if (ERROR_CODE_UNKNOWNDB.equals(sqlState)) {
                    result.setStatus(DB_UNKNOWNDB_FAILURE);
                } else if (ERROR_CODE_AUTH_FAILURE.equals(sqlState)) {
                    result.setStatus(DB_AUTHORIZATION_FAILURE);
                } else if ("08S01".equals(sqlState)) {
                    result.setStatus(DB_UNKNOWNHOST_FAILURE);
                } else {
                    result.setStatus(DB_UNKNOWN_FAILURE);
                }
            }
            logger.warning("Could not login to the database using " + dbUsername + ":" + dbPassword + "@" + dbHostname + "/" + dbName);
            logger.warning(e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
        return result;
    }

    public String checkDbVersion(String hostname, String dbName, String username, String password) {
        Connection conn = null;
        String dbVersion = null;
        try {
            conn = DriverManager.getConnection(makeConnectionString(hostname, dbName), username, password);
            dbVersion = checkDbVersion(conn);
        } catch (SQLException e) {}
        return dbVersion;
    }

    public DBActionsResult createDb(String privUsername, String privPassword, String dbHostname, String dbName, String dbUsername,
                                    String dbPassword, String dbCreateScript, boolean isWindows, boolean overwriteDb) throws IOException {
        DBActionsResult result = new DBActionsResult();

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(makeConnectionString(dbHostname, ADMIN_DB_NAME), privUsername, privPassword);
            stmt = conn.createStatement();
            boolean dbExists = testForExistingDb(stmt, dbName);
            if (dbExists) {
                if (!overwriteDb) {
                    result.setStatus(DB_ALREADY_EXISTS);
                }
                else {  //we should overwrite the db
                    dropDatabase(stmt, dbName);
                    makeDatabase(stmt, dbName, dbUsername, dbPassword, dbHostname, dbCreateScript, isWindows);
                    result.setStatus(DB_SUCCESS);
                }
            } else {
                makeDatabase(stmt, dbName, dbUsername, dbPassword, dbHostname, dbCreateScript, isWindows);
                result.setStatus(DB_SUCCESS);

            }
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if ("28000".equals(sqlState)) {
                result.setStatus(DB_AUTHORIZATION_FAILURE);
            }
            else if ("08S01".equals(sqlState)) {
                result.setStatus(DB_UNKNOWNHOST_FAILURE);
            } else {
                logger.warning("Could not create database. An exception occurred");
                logger.warning(e.getMessage());
                result.setStatus(DB_UNKNOWN_FAILURE);
                result.setErrorMessage(e.getMessage());
            }
        } finally {

                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                    }
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                    }
                }
        }

        return result;
    }

    public void dropDatabase(Statement stmt, String dbName) throws SQLException {
        stmt.executeUpdate(SQL_DROP_DB + dbName);
        logger.warning("dropping database \"" + dbName + "\"");
    }

    public void dropDatabase(String dbName, String hostname, String username, String password) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(makeConnectionString(hostname, ADMIN_DB_NAME), username, password);
            stmt = conn.createStatement();
            dropDatabase(stmt, dbName);
        } catch (SQLException e) {
            logger.severe("Failure while dropping the database: " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public DBActionsResult upgradeDbSchema(String hostname, String username, String password, String databaseName, String oldVersion,
                                           String newVersion) throws IOException {
        DBActionsResult result = new DBActionsResult();
        File f = new File(osFunctions.getPathToDBCreateFile());
        File parentDir = f.getParentFile();

        Map upgradeMap = buildUpgradeMap(parentDir);

        Connection conn = null;
        Statement stmt = null;

        try {
            conn = DriverManager.getConnection(makeConnectionString(hostname, databaseName), username, password);
            stmt = conn.createStatement();
            while (!oldVersion.equals(newVersion)) {
                String[] upgradeInfo = (String[]) upgradeMap.get(oldVersion);
                if (upgradeInfo == null) {
                    String msg = "no upgrade path from \"" + oldVersion + "\" to \"" + newVersion + "\"";
                    logger.warning(msg);
                    result.setStatus(DB_CANNOT_UPGRADE);
                    result.setErrorMessage(msg);
                } else {
                    logger.info("Upgrading \"" + databaseName + "\" from " + oldVersion + "->" + upgradeInfo[0]);

                    String upgradeScript = upgradeInfo[1];
                    String[] statements = getCreateDbStatements(upgradeScript);

                    conn.setAutoCommit(false);
                    for (int i = 0; i < statements.length; i++) {
                        String statement = statements[i];
                        stmt.executeUpdate(statement);
                    }
                    conn.commit();
                    conn.setAutoCommit(true);

                    oldVersion = checkDbVersion(conn);
                }
            }
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            result.setErrorMessage(e.getMessage());
            if (ERROR_CODE_AUTH_FAILURE.equals(sqlState)) {
                result.setStatus(DB_AUTHORIZATION_FAILURE);
            }
            else if (ERROR_CODE_UNKNOWNDB.equals(sqlState)) {
                result.setStatus(DB_UNKNOWNDB_FAILURE);
            }
            else {
                result.setStatus(DB_UNKNOWN_FAILURE);
            }
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

        return result;
    }


    public DbVersionChecker[] getDbVersionCheckers() {
        return dbCheckers;
    }

    public void setDbVersionCheckers(DbVersionChecker[] newCheckers) {
        dbCheckers = newCheckers;
    }

    //
    // PRIVATE METHODS
    //
    private void init() throws ClassNotFoundException {
        Class.forName(JDBC_DRIVER_NAME);
        ssgDbChecker = new CheckSSGDatabase();
        osFunctions = OSDetector.getOSSpecificFunctions();
        //always sort the dbCheckers in reverse in case someone has added one out of sequence so things still work properly
        Arrays.sort(dbCheckers, Collections.reverseOrder());
    }

    private Set<String> getTableColumns(String tableName, DatabaseMetaData metadata) throws SQLException {
        Set<String> columns = null;
        ResultSet rs = null;
        rs = metadata.getColumns(null, "%", tableName, "%");
        columns = new HashSet<String>();
        while(rs.next()) {
            String columnName = rs.getString("COLUMN_NAME");
            columns.add(columnName.toLowerCase());
        }
        return columns;
    }

    //will return the version of the DB, or null if it cannot be determined
    private String getDbVersion(Hashtable<String, Set> tableData) {
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
    private String checkDbVersion(Connection conn) throws SQLException {
        Hashtable<String, Set> tableData = collectMetaInfo(conn);
        //now we have a hashtable of tables and their columns
        return getDbVersion(tableData);
    }

    private Hashtable<String, Set> collectMetaInfo(Connection conn) throws SQLException {
        Hashtable<String, Set> tableData = new Hashtable<String, Set>();

        DatabaseMetaData metadata = conn.getMetaData();
        String[] tableTypes = {
                "TABLE",
        };

        ResultSet tableNames = metadata.getTables(null, "%", "%", tableTypes);
        while (tableNames.next()) {
            String tableName = tableNames.getString("TABLE_NAME");
            try {
                Set<String> columns = getTableColumns(tableName, metadata);
                if (columns != null) {
                    tableData.put(tableName.toLowerCase(), columns);
                }
            } catch (SQLException ex) {
            }
        }
        return tableData;
    }

    private void makeDatabase(Statement stmt, String dbName, String username, String password, String hostname, String dbCreateScript, boolean isWindows) throws SQLException, IOException {
        stmt.getConnection().setAutoCommit(false); //start transaction
        logger.info("creating database \"" + dbName +"\"");
        stmt.executeUpdate(SQL_CREATE_DB + dbName);
        stmt.execute(SQL_USE_DB + dbName);
        String[] sqlArray = getCreateDbStatements(dbCreateScript);
        if (sqlArray != null) {
            logger.info("Creating schema for " + dbName + " database");
            for (int i = 0; i < sqlArray.length; i++) {
                String s = sqlArray[i];
                stmt.executeUpdate(s);
            }

            logger.info("Creating user \"" + username + "\" and performing grants on " + dbName + " database");
            String[] grantStmts = getGrantStatements(dbName, hostname, username, password, isWindows);
            if (grantStmts != null) {
                for (int j = 0; j < grantStmts.length; j++) {
                    String grantStmt = grantStmts[j];
                    stmt.executeUpdate(grantStmt);
                }
            }
        }

        stmt.getConnection().commit();     //finish transaction
        stmt.getConnection().setAutoCommit(true);
    }

    private String[] getGrantStatements(String dbName, String hostname, String username, String password, boolean isWindows) {
        List<String> list = new ArrayList<String>();
        list.add(new String(SQL_GRANT_ALL + dbName + ".* to " + username + "@'%' identified by '" + password + "'"));
        list.add(new String(SQL_GRANT_ALL + dbName + ".* to " + username + "@" + hostname + " identified by '" + password + "'"));

        if (!isWindows && hostname.equals("localhost")) {
            list.add(new String(SQL_GRANT_ALL + dbName + ".* to " + username + "@localhost.localdomain identified by '" + password + "'"));
        }
        list.add(new String("FLUSH PRIVILEGES"));
        return list.toArray(new String[]{});
    }

    private String[] getCreateDbStatements(String dbCreateScript) throws IOException {
        String []  stmts = null;
        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            StringBuffer sb = new StringBuffer();

            fileReader = new FileReader(dbCreateScript);
            reader = new BufferedReader(fileReader);
            String temp = null;
            while((temp = reader.readLine()) != null) {
                if (!temp.startsWith("--") && !temp.equals("")) {
                    sb.append(temp);
                }
            }
            Pattern splitPattern = Pattern.compile(";");
            stmts = splitPattern.split(sb.toString());
        } finally{
            if (reader != null) {
                if (fileReader != null) {
                    fileReader.close();
                }

                if (reader != null) {
                    reader.close();
                }
            }
        }
        return stmts;
    }



    private boolean testForExistingDb(Statement stmt, String dbName) {
        boolean dbExists = false;
        try {
            stmt.execute(SQL_USE_DB + dbName);
            dbExists = true;       //if an exception was not thrown then the db must already exist.
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if ("42000".equals(sqlState)) {     //this means the db didn't exist. In this case, the exception is what we want.
                dbExists = false;
            }
        }
        return dbExists;
    }

    private String makeConnectionString(String hostname, String dbName) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(MYSQL_CONNECTION_PREFIX).append(hostname).append("/").append(dbName);
        return buffer.toString();
    }


    private Map buildUpgradeMap(File parentDir) {
        File[] upgradeScripts = parentDir.listFiles(new FilenameFilter() {
                public boolean accept(File file, String s) {
                    return s.toUpperCase().startsWith("UPGRADE") &&
                            s.toUpperCase().endsWith("SQL");
                }
        });

        Pattern upgradePattern = Pattern.compile(UPGRADE_SQL_PATTERN);
        Map<String, String[]> upgradeMap = new HashMap<String, String[]>();

        for (int i = 0; i < upgradeScripts.length; i++) {
            File upgradeScript = upgradeScripts[i];
            String filename = upgradeScript.getName();
            Matcher matcher = upgradePattern.matcher(filename);
            if (matcher.matches()) {
                String startVersion = matcher.group(1);
                String destinationVersion = matcher.group(2);
                upgradeMap.put(startVersion, new String[]{destinationVersion, upgradeScript.getAbsolutePath()});
            }
        }
        return upgradeMap;
    }

    public boolean doCreateDb(String pUsername, String pPassword, String hostname, String name, String username, String password, boolean overwriteDb, DBActionsListener ui) {
        //check if the root username is "" or null, or the password is null. Password is allowed to be "", if there isn't a password
        if (StringUtils.isEmpty(pUsername) || pPassword == null
                ) {
            if (ui != null) {
                String defaultUserName = StringUtils.isEmpty(pUsername)?"root":pUsername;
                Map<String, String> creds = ui.getPrivelegedCredentials(
                            "Please enter the credentials for the root database user (needed to create a database)",
                            "Please enter the username for the root database user (needed to create a database): [" + defaultUserName + "] ",
                            "Please enter the password for the root database user: ",
                            defaultUserName);

                if (creds != null) {
                    pUsername = creds.get(USERNAME_KEY);
                    pPassword = creds.get(PASSWORD_KEY);
                } else {
                    return false;
                }
            }
        }

        String errorMsg;
        boolean isOk = false;

        String dbCreateScriptFile = osFunctions.getPathToDBCreateFile();
        boolean isWindows = osFunctions.isWindows();

        DBActionsResult result = null;
        try {
            logger.info("Attempting to create a new database (" + hostname + "/" + name + ") using privileged user \"" + pUsername + "\"");

            result = createDb(pUsername, pPassword, hostname, name, username, password, dbCreateScriptFile, isWindows, overwriteDb);
            int status = result.getStatus();
            if ( status == DBActions.DB_SUCCESS) {
                isOk = true;
                if (ui != null) {
                    ui.showSuccess("Database Successfully Created\n");
                }
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
            errorMsg = "Could not create the database because there was an error while reading the file \"" + dbCreateScriptFile + "\"." +
                    " The error was: " + e.getMessage();
            logger.warning(errorMsg);
            if (ui != null) ui.showErrorMessage(errorMsg);
            isOk = false;
        }

        return isOk;
    }

    public boolean doExistingDb(String dbName, String hostname, String username, String password, String privUsername, String privPassword, String currentVersion, DBActionsListener ui) {
        String errorMsg;
        boolean isOk = false;

        DBActions.DBActionsResult status;
        logger.info("Attempting to connect to an existing database (" + hostname + "/" + dbName + ")" + "using username/password \"" + username + "/" + password + "\"");
        status = checkExistingDb(hostname, dbName, username, password);
        if (status.getStatus() == DBActions.DB_SUCCESS) {
            logger.info(CONNECTION_SUCCESSFUL_MSG);
            logger.info("Now Checking database version");
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
                            privPassword = creds.get(DBActions.PASSWORD_KEY);

                            isOk = doDbUpgrade(hostname, dbName, currentVersion, dbVersion, privUsername, privPassword, ui);
                            if (isOk && ui != null) ui.showSuccess("Database Successfully Upgraded\n");
                        } catch (IOException e) {
                            errorMsg = "There was an error while attempting to upgrade the database";
                            logger.severe(errorMsg);
                            logger.severe(e.getMessage());
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
                    errorMsg = "There was an authentication error when attempting to connect to the database \"" + dbName + "\" using the username \"" +
                            username + "\". Perhaps the password is wrong. Please check your input and try again.";
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

    private boolean doDbUpgrade(String hostname, String dbName, String currentVersion, String dbVersion, String privUsername, String privPassword, DBActionsListener ui) throws IOException {
        boolean isOk = false;

        logger.info("Attempting to upgrade the existing database \"" + dbName+ "\"");
        DBActions.DBActionsResult upgradeResult = upgradeDbSchema(hostname, privUsername, privPassword, dbName, dbVersion, currentVersion);
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
                msg = "Could not connect to the host: \"" + hostname + "\". Please check the hostname and try again.";
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
        return isOk;
    }


}