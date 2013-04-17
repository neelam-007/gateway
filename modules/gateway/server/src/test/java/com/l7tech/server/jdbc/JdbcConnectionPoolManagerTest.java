package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.util.Pair;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.Before;
import org.junit.Test;


import java.util.Collections;

import static org.junit.Assert.assertTrue;

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

    @Test
    public void testInvalidDriverClass() throws Exception {
        JdbcConnection jdbcConnection = new JdbcConnection();
        jdbcConnection.setName("TEST");
        jdbcConnection.setDriverClass("com.test.driver");

        ApplicationContexts.inject(jdbcConnectionPoolManager, Collections.singletonMap("serverConfig", new ServerConfigStub()));
        Pair<ComboPooledDataSource, String> result = jdbcConnectionPoolManager.updateConnectionPool(jdbcConnection, false);
        assertTrue("No datasource created", result.left == null);
        assertTrue("Driver class not supported", result.right.contains("Driver class com.test.driver is not supported."));
    }

}
