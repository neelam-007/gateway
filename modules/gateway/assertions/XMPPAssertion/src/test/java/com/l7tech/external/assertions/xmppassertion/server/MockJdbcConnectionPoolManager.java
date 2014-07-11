package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * User: ashah
 * Date: 24/04/12
 * Time: 5:04 PM
 */
public class MockJdbcConnectionPoolManager extends JdbcConnectionPoolManager{

    public MockJdbcConnectionPoolManager(final JdbcConnectionManager jdbcConnectionManager){
        super(jdbcConnectionManager);
    }
    public DataSource getDataSource(String jdbcConnName) throws NamingException, SQLException {
        return new MockDataSource();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }
}
