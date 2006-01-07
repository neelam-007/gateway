package com.l7tech.server.config;

import com.l7tech.server.config.exceptions.UnsupportedOsException;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
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
    public static final int DB_CANNOT_UPGRADE = -4;

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

    public int checkExistingDb(String dbHostname, String dbName, String dbUsername, String dbPassword) /*throws WrongDbVersionException*/ {
        int failureCode = DB_UNKNOWN_FAILURE;
        String connectionString = makeConnectionString(dbHostname, dbName);
        String dbVersion = null;
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(connectionString, dbUsername, dbPassword);
            failureCode = DB_SUCCESS;
            //dbVersion = checkDbVersion(conn);

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

    private String checkDbVersion(Connection conn) throws SQLException {
        String dbVersion = null;

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            if (stmt != null) {
                try {
                    stmt.executeQuery(SQL_DETECT_SSG_DATABASE);
                    //now that the db is at least found, lets check it's version
                    HashSet tableNames = null;
                    try {
                        ResultSet rs = stmt.executeQuery("show tables");
                        tableNames = new HashSet();
                        while(rs.next()) {
                            tableNames.add(rs.getString(1));
                        }
                    } catch (SQLException e) {
                        logger.severe("could not obtain a listing of tables. Cannot determine database version");
                    }

                    if (tableNames != null && tableNames.contains("cluster_properties")) {
                        //then this is a 3.3+ database

                        try {
                            stmt.execute("select http_methods from published_service");
                            dbVersion = "4.0";
                        } catch (SQLException e1) {
                            try {
                                stmt.execute("select ski from client_cert");
                                dbVersion = "3.4";
                            } catch (SQLException e2) {
                                dbVersion = "3.3";
                            }
                        }
                    } else {
                        //check the contents of the audit_message table to see if this is a 3.2 or a 3.1 db
                        try {
                            stmt.execute("select operation_name from audit_message");
                            stmt.execute("select response_status from audit_message");
                            stmt.execute("select routing_latency from audit_message");
                            dbVersion = "3.2";
                        } catch (SQLException sqlex) {
                            dbVersion = "3.1";
                        }
                    }
                } catch (SQLException e1) {
                    logger.warning("no SSG database found");
                }
            }
        } catch (SQLException e) {
            logger.severe("Could not check the version of the DB");
            logger.severe(e.getMessage());
            throw e;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                }
            }
        }
        return dbVersion;
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

    public int upgradeDbSchema(String hostname, String username, String password, String databaseName, String oldVersion, String newVersion, OSSpecificFunctions osFunctions) throws IOException {
        int status = DB_SUCCESS;
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
                    logger.warning("no upgrade path from \"" + oldVersion + "\" to \"" + newVersion + "\"");
                    status = DB_CANNOT_UPGRADE;
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
            if (ERROR_CODE_AUTH_FAILURE.equals(sqlState)) {
                status = DB_AUTHORIZATION_FAILURE;
            }
            else if (ERROR_CODE_UNKNOWNDB.equals(sqlState)) {
                status = DB_UNKNOWNDB_FAILURE;
            }
            else {
                status = DB_UNKNOWN_FAILURE;
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

        return status;
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

    public static void main(String[] args) {

        final String currentVersion = "3.4";

        boolean doFullTest = false;

        if (args.length > 0) {
            String arg = args[0];
            if (arg.equals("-full")) {
                 doFullTest = true;
            } else if (arg.equals("-create")) {
                doFullTest = false;
            } else {
                doFullTest = false;
            }
        }

        String hostname = "localhost";
        String dbName = "ssg";
        String privUsername = "root";
        String privPassword = "7layer";
        String username = "gateway";
        String password = "7layer";

        OSSpecificFunctions osFunctions = null;
        try {
            osFunctions = OSDetector.getOSSpecificActions();
        } catch (UnsupportedOsException e) {
            e.printStackTrace();
            System.exit(1);
        }

        boolean isWindows = osFunctions.isWindows();

        String[] versions = new String[] {"3.1", "3.2", "3.3", currentVersion};
        try {
            DBActions dbActions = new DBActions();


            int success = DB_SUCCESS;
            for (int i = 0; i < versions.length; i++) {
                String realVersion = versions[i];
                String versionName = realVersion.replaceAll("\\.", "");
                System.out.println("Creating database - version " + realVersion);
                success = dbActions.createDb(privUsername, privPassword, hostname, dbName+versionName, username, password, "ssg"+realVersion +".sql", isWindows, true);
                if (success == DB_SUCCESS) {
                    System.out.println("Success creating database - version " + realVersion);
                }
                else {
                    System.out.println("Could not create database realVersion: " + realVersion);
                    success = DB_UNKNOWN_FAILURE;
                }
            }

            if (success == DB_SUCCESS) {
                String dbVersion = null;
                for (int i = 0; i < versions.length; i++) {
                    String realVersion = versions[i];
                    String versionName = realVersion.replaceAll("\\.", "");

                    dbVersion = dbActions.checkDbVersion(hostname, dbName+versionName, username, password);
                    if (dbVersion != null && dbVersion.equals(realVersion)) {
                        System.out.println(dbName+versionName + " version is correct. Wanted " + realVersion + " and detected " + dbVersion);
                    } else {
                        System.out.println(dbName+versionName + " has a problem with the version. Wanted " + realVersion + " but detected " + dbVersion);
                    }

                    if (!currentVersion.equals(dbVersion) && doFullTest) {
                        System.out.println("Upgrading " + dbName+versionName + " from " + dbVersion + " to " + currentVersion);
                        int upgradeStatus = dbActions.upgradeDbSchema(hostname, privUsername, privPassword, dbName+versionName, dbVersion, currentVersion, osFunctions);
                        if (upgradeStatus == DB_SUCCESS) {
                            System.out.println("Successful upgrade procedure!!");
                            System.out.println("Checking version again");
                            dbVersion = dbActions.checkDbVersion(hostname, dbName+versionName, username, password);
                            if (dbVersion.equals(currentVersion)) {
                                System.out.println("The version of the upgraded DB is correct");
                            } else {
                                System.out.println("The version of the upgraded DB is incorrect - detected version " + dbVersion + " should be " + currentVersion);
                            }
                        } else {
                            System.out.println("Failed upgrade procedure!!");
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}