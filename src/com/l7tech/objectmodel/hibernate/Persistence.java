package com.l7tech.objectmodel.hibernate;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 7-May-2003
 * Time: 12:06:26 PM
 * To change this template use Options | File Templates.
 */
public interface Persistence {
    public void init() throws SQLException;
    public Connection getConnection() throws SQLException;
}
