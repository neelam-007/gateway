/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author alex
 */
public class MysqlDriverWithNoAutoCommit extends com.mysql.jdbc.Driver {
    public Connection connect( String s, Properties p ) throws SQLException {
        Connection conn = super.connect( s, p );
        conn.setAutoCommit(false);
        return conn;
    }

    public MysqlDriverWithNoAutoCommit() throws SQLException {
        super();
    }
}
