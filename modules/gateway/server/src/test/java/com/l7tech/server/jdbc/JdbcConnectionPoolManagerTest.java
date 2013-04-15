package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import org.junit.Before;
import org.junit.Test;

import javax.naming.NameNotFoundException;

import static org.junit.Assert.assertNotNull;

public class JdbcConnectionPoolManagerTest {

    private JdbcConnectionPoolManager jdbcConnectionPoolManager;

    @Before
    public void setUp() throws Exception {

        JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setName("Mysql");
        jdbcConnection.setDriverClass("com.mysql.jdbc.Driver");

        final JdbcConnectionManager jdbcConnectionManager = new JdbcConnectionManagerStub(jdbcConnection);
        jdbcConnectionPoolManager = new JdbcConnectionPoolManager(jdbcConnectionManager);
        jdbcConnectionPoolManager.afterPropertiesSet();

    }

    @Test(expected = NullPointerException.class)
    public void testInvalidDriverClass() throws Exception {
        JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setName("TEST");
        jdbcConnection.setDriverClass("com.test.driver");
        jdbcConnectionPoolManager.updateConnectionPool(jdbcConnection, false);
    }

}
