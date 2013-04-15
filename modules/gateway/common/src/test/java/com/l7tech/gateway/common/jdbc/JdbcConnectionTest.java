package com.l7tech.gateway.common.jdbc;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JdbcConnectionTest {
    private JdbcConnection connection;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        connection = new JdbcConnection();
    }

    @Test
    public void copyFromSetsSecurityZone() {
        connection.setSecurityZone(zone);
        final JdbcConnection copy = new JdbcConnection();
        copy.copyFrom(connection);
        assertEquals(zone, copy.getSecurityZone());
    }
}
