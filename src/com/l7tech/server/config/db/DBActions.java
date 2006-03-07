package com.l7tech.server.config.db;

import com.l7tech.server.config.OSSpecificFunctions;

import java.io.*;
import java.sql.*;
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

    private static final String ERROR_CODE_AUTH_FAILURE = "28000";
    private static final String ERROR_CODE_UNKNOWNDB = "42000";

    public static final int DB_AUTHORIZATION_FAILURE = 28000;
    public static final int DB_UNKNOWNDB_FAILURE = 42000;

    public static final int DB_UNKNOWN_FAILURE = -1;
    public static final int DB_CHECK_INTERNAL_ERROR = -2;
    public static final int DB_UNKNOWNHOST_FAILURE = -3;
    public static final int DB_CANNOT_UPGRADE = -4;

    private static final String JDBC_DRIVER_NAME = "com.mysql.jdbc.Driver";
    private static final String ADMIN_DB_NAME = "mysql";

    private static final String SQL_CREATE_DB = "create database ";
    private static final String SQL_USE_DB = "use ";
    private static final String SQL_GRANT_ALL = "grant all on ";
    private static final String SQL_DROP_DB = "drop database ";

    private CheckSSGDatabase ssgDbChecker;

    private DbVersionChecker[] dbCheckers = new DbVersionChecker[] {
        new DbVersion40Checker(),
        new DbVersion35Checker(),
        new DbVersion34Checker(),
        new DbVersion33Checker(),
        new DbVersion3132Checker(),
    };

    public static class DBActionsResult {
        private int status = 0;
        private String errorMessage = null;

//        public DBActionsResult(int status, String errorMessage) {
//            this.status = status;
//            this.errorMessage = errorMessage;
//        }

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
        //int failureCode = DB_UNKNOWN_FAILURE;
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
        } catch (SQLException e) {
        }
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
            e.printStackTrace();
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
                                           String newVersion, OSSpecificFunctions osFunctions) throws IOException {
        DBActionsResult result = new DBActionsResult();
        File f = new File(osFunctions.getPathToDBCreateFile());
        File parentDir = f.getParentFile();

        HashMap upgradeMap = buildUpgradeMap(parentDir);

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

        //always sort the dbCheckers in reverse in case someone has added one out of sequence so things still work properly
        Arrays.sort(dbCheckers, Collections.reverseOrder());
    }

    private Set getTableColumns(String tableName, DatabaseMetaData metadata) throws SQLException {
        Set columns = null;
        ResultSet rs = null;
        rs = metadata.getColumns(null, "%", tableName, "%");
        columns = new HashSet();
        while(rs.next()) {
            String columnName = rs.getString("COLUMN_NAME");
            columns.add(columnName.toLowerCase());
        }
        return columns;
    }

    //will return the version of the DB, or null if it cannot be determined
    private String getDbVersion(Hashtable tableData) {
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
        Hashtable tableData = collectMetaInfo(conn);
        //now we have a hashtable of tables and their columns
        return getDbVersion(tableData);
    }

    private Hashtable collectMetaInfo(Connection conn) throws SQLException {
        Hashtable tableData = new Hashtable();

        DatabaseMetaData metadata = conn.getMetaData();
        String[] tableTypes = {
                "TABLE",
        };

        ResultSet tableNames = metadata.getTables(null, "%", "%", tableTypes);
        while (tableNames.next()) {
            String tableName = tableNames.getString("TABLE_NAME");
            try {
                Set columns = getTableColumns(tableName, metadata);
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
        ArrayList list = new ArrayList();
        list.add(new String(SQL_GRANT_ALL + dbName + ".* to " + username + "@'%' identified by '" + password + "'"));
        list.add(new String(SQL_GRANT_ALL + dbName + ".* to " + username + "@" + hostname + " identified by '" + password + "'"));

        if (!isWindows && hostname.equals("localhost")) {
            list.add(new String(SQL_GRANT_ALL + dbName + ".* to " + username + "@localhost.localdomain identified by '" + password + "'"));
        }
        list.add(new String("FLUSH PRIVILEGES"));
        return (String[]) list.toArray(new String[list.size()]);
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


    private HashMap buildUpgradeMap(File parentDir) {
        File[] upgradeScripts = parentDir.listFiles(new FilenameFilter() {
                public boolean accept(File file, String s) {
                    return s.toUpperCase().startsWith("UPGRADE") &&
                            s.toUpperCase().endsWith("SQL");
                }
        });

        Pattern upgradePattern = Pattern.compile("^upgrade_(.*)-(.*).sql$");
        HashMap upgradeMap = new HashMap();

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
}