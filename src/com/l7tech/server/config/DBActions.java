package com.l7tech.server.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 25, 2005
 * Time: 8:06:24 AM
 * To change this template use File | Settings | File Templates.
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


    private int maxRetryCount;
    private int retryCount;

    private static final String JDBC_DRIVER_NAME = "com.mysql.jdbc.Driver";
    private static final String ADMIN_DB_NAME = "mysql";

    private static final String SQL_CREATE_DB = "create database ";
    private static final String SQL_USE_DB = "use ";
    private static final String SQL_GRANT_ALL = "grant all on ";
    private static final String SQL_DROP_DB = "drop database ";
    private static final String SQL_DETECT_VERSION_33 = "select 1 from community_schemas";
    private static final String SQL_DETECT_SSG_DATABASE = "select 1 from published_service";

    public static final String PRE_33_VERSION_MSG = "The existing database appears to be pre version 3.3";
    public static final String UPGRADE_DB_MSG = "If the SSG fails to start, you may need to upgrade the database";

    private static final String ERROR_CODE_UNKNOWNDB = "42000";
    private static final String ERROR_CODE_AUTH_FAILURE = "28000";


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


    public DBActions(int maxRetryCount) throws ClassNotFoundException {
        this.maxRetryCount = maxRetryCount;
        retryCount = 0;
        init();
    }

    public DBActions() throws ClassNotFoundException {
        init();

    }

    private void init() throws ClassNotFoundException {
        Class.forName(JDBC_DRIVER_NAME);
    }

    public int checkExistingDb(String dbHostname, String dbName, String dbUsername, String dbPassword) throws WrongDbVersionException {
        int failureCode = DB_UNKNOWN_FAILURE;
        Connection conn = null;
        String connectionString = makeConnectionString(dbHostname, dbName);
        try {
            conn = DriverManager.getConnection(connectionString, dbUsername, dbPassword);
            failureCode = checkDbVersion(conn);
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if (sqlState != null) {
                if (ERROR_CODE_UNKNOWNDB.equals(sqlState)) {
                    failureCode = DB_UNKNOWNDB_FAILURE;
                } else if (ERROR_CODE_AUTH_FAILURE.equals(sqlState)) {
                    failureCode = DB_AUTHORIZATION_FAILURE;
                } else if ("08S01".equals(sqlState)) {
                    failureCode = DB_UNKNOWNHOST_FAILURE;
                } else {
                    failureCode = DB_UNKNOWN_FAILURE;
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
        return failureCode;
    }

    private int checkDbVersion(Connection conn) throws WrongDbVersionException {
        int status = DB_INCORRECT_VERSION;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            try {
                stmt.executeQuery(SQL_DETECT_SSG_DATABASE);
                try {
                    stmt.executeQuery(SQL_DETECT_VERSION_33);
                    status = DB_SUCCESS;
                } catch (SQLException e) {
                    throw new WrongDbVersionException("3.2", PRE_33_VERSION_MSG);
//                    logger.warning(PRE_33_VERSION_MSG);
//                    logger.warning(UPGRADE_DB_MSG);
//                    status = DB_INCORRECT_VERSION;
                }
            } catch(SQLException e) {
                status = DB_INCORRECT_VERSION;
            }
        } catch (SQLException e) {
            status = DB_CHECK_INTERNAL_ERROR;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                }
            }
        }

        return status;
    }

    public void resetRetryCount() {
        retryCount = 0;
    }

    public int createDb(String privUsername, String privPassword, String dbHostname, String dbName, String dbUsername, String dbPassword, String dbCreateScript, boolean isWindows, boolean overwriteDb) throws IOException {
        int failureCode = DBActions.DB_UNKNOWN_FAILURE;

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(makeConnectionString(dbHostname, ADMIN_DB_NAME), privUsername, privPassword);
            stmt = conn.createStatement();
            boolean dbExists = testForExistingDb(stmt, dbName);
            if (dbExists) {
                if (!overwriteDb) {
                    failureCode = DB_ALREADY_EXISTS;
                }
                else {  //we should overwrite the db
                    dropDatabase(stmt, dbName);
                    makeDatabase(stmt, dbName, dbUsername, dbPassword, dbHostname, dbCreateScript, isWindows);
                    failureCode = DB_SUCCESS;
                }
            } else {
                makeDatabase(stmt, dbName, dbUsername, dbPassword, dbHostname, dbCreateScript, isWindows);
                failureCode = DB_SUCCESS;

            }
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if ("28000".equals(sqlState)) {
                failureCode = DB_AUTHORIZATION_FAILURE;
            }
            else if ("08S01".equals(sqlState)) {
                failureCode = DB_UNKNOWNHOST_FAILURE;
            } else {
                logger.warning("Could not create database. An exception occurred");
                logger.warning(e.getMessage());
                failureCode = DB_UNKNOWN_FAILURE;
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

        return failureCode;
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
                if (temp.startsWith("--") || temp.equals("")) {
                    continue;
                } else {
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

    private void dropDatabase(Statement stmt, String dbName) throws SQLException {
        stmt.executeUpdate(SQL_DROP_DB + dbName);
        logger.warning("dropping database \"" + dbName + "\"");
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
}