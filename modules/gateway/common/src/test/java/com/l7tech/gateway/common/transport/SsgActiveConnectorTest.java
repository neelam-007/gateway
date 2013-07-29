package com.l7tech.gateway.common.transport;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Test;

import static org.junit.Assert.*;

public class SsgActiveConnectorTest {
    @Test
    public void copyConstructorSetsSecurityZone() {
        final SecurityZone zone = new SecurityZone();
        final SsgActiveConnector conn = new SsgActiveConnector();
        conn.setSecurityZone(zone);
        final SsgActiveConnector copy = new SsgActiveConnector(conn);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void copyConstructorSetsOldOid() {
        final SsgActiveConnector conn = new SsgActiveConnector();
        conn.setOldOid(1234L);
        final SsgActiveConnector copy = new SsgActiveConnector(conn);
        assertEquals(new Long(1234L), copy.getOldOid());
        assertEquals(conn,copy);
    }
}
