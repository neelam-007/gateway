/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.PrintWriter;

/**
 * @author alex
 */
public class MysqlDataSourceWithNoAutoCommit extends com.mysql.jdbc.jdbc2.optional.MysqlDataSource {
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        conn.setAutoCommit(false);
        return conn;
    }

    public Connection getConnection( String username, String password ) throws SQLException {
        Connection conn = super.getConnection( username, password );
        conn.setAutoCommit(false);
        return conn;
    }
}
