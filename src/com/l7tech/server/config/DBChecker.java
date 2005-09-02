package com.l7tech.server.config;

import com.l7tech.server.config.gui.ConfigWizardDatabasePanel;

import javax.swing.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 25, 2005
 * Time: 8:06:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class DBChecker {
    private static final Logger logger = Logger.getLogger(DBChecker.class.getName());

    public static final int DB_SUCCESS = 0;
    public static final int DB_AUTH_FAILURE = -1;
    public static final int DB_MISSING_FAILURE = -2;
    public static final int DB_MAX_RETRIES_EXCEEDED = -3;
    public static final int DB_CHECK_INTERNAL_ERROR = -4;

    private int maxRetryCount;
    private int retryCount;

    private static final String JDBC_DRIVER_NAME = "com.mysql.jdbc.Driver";

    public DBChecker(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        retryCount = 0;
    }


    public int checkDb(String connectionString, String name, String password) {
        int failureCode = DB_SUCCESS;
        //if (retryCount < maxRetryCount) {
            Connection conn = null;
            try {
                Class.forName(JDBC_DRIVER_NAME);
                conn = DriverManager.getConnection(connectionString, name, password);
            } catch (ClassNotFoundException e) {
                logger.log(Level.SEVERE, "Could not locate the MySQL driver in the classpath", e.getException());
                logger.warning(String.valueOf(maxRetryCount - retryCount) + " retries remaining");
                failureCode = DB_CHECK_INTERNAL_ERROR;
                retryCount++;
            } catch (SQLException e) {
                logger.warning("Could not login to the database using the following connection string:");
                logger.warning(connectionString);
                logger.warning(String.valueOf(maxRetryCount - retryCount) + " retries remaining");
                failureCode = DB_AUTH_FAILURE;
                retryCount++;
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                    }
                }
            }
//        } else {
          if (retryCount > maxRetryCount) {
                logger.warning("Maximum database connection attempts ("  + String.valueOf(maxRetryCount) + ") exceeded.");
                failureCode = DB_MAX_RETRIES_EXCEEDED;
          }

//        }
        return failureCode;
    }


    public void resetRetryCount() {
        retryCount = 0;
    }
}
