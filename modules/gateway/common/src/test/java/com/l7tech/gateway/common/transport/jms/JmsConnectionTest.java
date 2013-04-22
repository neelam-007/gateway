package com.l7tech.gateway.common.transport.jms;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JmsConnectionTest {
    private JmsConnection conn;
    private SecurityZone zone;

    @Before
    public void setup() {
        conn = new JmsConnection();
        zone = new SecurityZone();
    }

    @Test
    public void copyFromSetsSecurityZone() {
        conn.setSecurityZone(zone);
        final JmsConnection copy = new JmsConnection();
        copy.copyFrom(conn);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void copyFromSetsNullSecurityZone() {
        conn.setSecurityZone(null);
        final JmsConnection copy = new JmsConnection();
        copy.setSecurityZone(new SecurityZone());
        copy.copyFrom(conn);
        assertNull(copy.getSecurityZone());
    }

    @Test
    public void copyConstructor() {
        conn.setSecurityZone(zone);
        final JmsConnection copy = new JmsConnection(conn, false);
        assertEquals(zone, copy.getSecurityZone());
    }
}
