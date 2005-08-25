package com.l7tech.server.config;

import javax.swing.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 25, 2005
 * Time: 8:06:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class DBChecker {
    public static final int DB_SUCCESS = 0;
    public static final int DB_AUTH_FAILURE = -1;
    public static final int DB_MISSING_FAILURE = -2;
    public static final int DB_MAX_RETRIES_EXCEEDED = -3;

    private int maxRetryCount;
    private int retryCount;

    public DBChecker(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        retryCount = 0;
    }


    public int checkDb(String connectionString, String name, String password) {
        int failureCode = DB_SUCCESS;
        if (retryCount < maxRetryCount) {
            ++retryCount;
            Connection conn;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                conn = DriverManager.getConnection(connectionString, name, password);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                failureCode = DB_AUTH_FAILURE;
                //e.printStackTrace(); LOG THIS?
            }
        } else {
            failureCode = DB_MAX_RETRIES_EXCEEDED;
        }
        return failureCode;
    }


}
