package com.l7tech.server.config;

import com.l7tech.server.config.gui.ConfigWizardDatabasePanel;
import com.l7tech.server.config.beans.NewDatabaseConfigBean;
import com.l7tech.server.config.exceptions.UnsupportedOsException;

import javax.swing.*;
import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.io.*;

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
    public static final int DB_AUTHORIZATION_FAILURE = 28000;
    public static final int DB_UNKNOWNDB_FAILURE = 42000;

    public static final int DB_UNKNOWN_FAILURE = -1;
    public static final int DB_CHECK_INTERNAL_ERROR = -2;

    private int maxRetryCount;
    private int retryCount;

    private static final String JDBC_DRIVER_NAME = "com.mysql.jdbc.Driver";
    private static final String ADMIN_DB_NAME = "mysql";

    private Connection conn = null;
    private static final String SQL_CREATE_DB = "create database ";
    private static final String SQL_USE_DB = "use ";
    private static final String SQL_GRANT_ALL = "grant all on ";
    private static final String SQL_DROP_DB = "drop database ";

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

    public int checkExistingDb(String dbHostname, String dbName, String dbUsername, String dbPassword) {
        int failureCode = DB_UNKNOWN_FAILURE;
        Connection conn = null;
        String connectionString = makeConnectionString(dbHostname, dbName);
        try {
            conn = DriverManager.getConnection(connectionString, dbUsername, dbPassword);
            failureCode = DB_SUCCESS;
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if (sqlState != null) {
                if ("42000".equals(sqlState)) {
                    failureCode = DB_UNKNOWNDB_FAILURE;

                } else if ("28000".equals(sqlState)) {
                    failureCode = DB_AUTHORIZATION_FAILURE;
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

    public void resetRetryCount() {
        retryCount = 0;
    }

    public int createDb(String privUsername, String privPassword, String dbHostname, String dbName, String dbUsername, String dbPassword, String dbCreateScript, boolean isWindows, boolean overwriteDb) {
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
                    try {
                        makeDatabase(stmt, dbName, dbUsername, dbPassword, dbHostname, dbCreateScript, isWindows);
                        failureCode = DB_SUCCESS;
                    } catch (IOException e) {
                    }
                }
            } else {
                try {
                    makeDatabase(stmt, dbName, dbUsername, dbPassword, dbHostname, dbCreateScript, isWindows);
                    failureCode = DB_SUCCESS;
                } catch (IOException e) {
                    failureCode = DB_CHECK_INTERNAL_ERROR;
                }
            }
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if ("28000".equals(sqlState)) {
                failureCode = DB_AUTHORIZATION_FAILURE;
            } else {
                logger.warning("Could not create database. An exception occurred");
                logger.warning(e.getMessage());
                failureCode = DB_UNKNOWN_FAILURE;
            }
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
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
        BufferedReader reader = null;
        try {
            StringBuffer sb = new StringBuffer();
            reader = new BufferedReader(new FileReader(dbCreateScript));
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
                try {
                    reader.close();
                } catch (IOException e) {
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