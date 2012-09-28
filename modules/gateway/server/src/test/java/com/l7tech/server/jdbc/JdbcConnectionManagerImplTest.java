package com.l7tech.server.jdbc;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class JdbcConnectionManagerImplTest {

    private JdbcConnectionManager connectionManager;

    @Before
    public void setUp() {
        connectionManager = new JdbcConnectionManagerImpl();
    }

    @Test
    public void testDriverClassNotSupported() throws Exception {
        assertFalse(connectionManager.isDriverClassSupported("test.driver"));
    }

    @Test
    public void testDriverClassSupported() throws Exception {
        assertTrue(connectionManager.isDriverClassSupported("com.mysql.jdbc.Driver"));
    }
}
