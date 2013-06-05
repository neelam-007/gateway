package com.l7tech.objectmodel;

import org.junit.Test;

import static org.junit.Assert.*;

public class ZoneableEntityHeaderTest {
    private static final long OID = 1234L;
    private static final String NAME = "test";
    private static final String DESC = "desc test";
    private static final Integer VER = 1;

    @Test
    public void copyConstructor() {
        final EntityHeader entityHeader = new EntityHeader(OID, EntityType.POLICY, NAME, DESC, VER);
        final ZoneableEntityHeader zoneableHeader = new ZoneableEntityHeader(entityHeader);
        assertEquals(OID, zoneableHeader.getOid());
        assertEquals(NAME, zoneableHeader.getName());
        assertEquals(DESC, zoneableHeader.getDescription());
        assertEquals(VER, zoneableHeader.getVersion());
        assertEquals(EntityType.POLICY, zoneableHeader.getType());
        assertNull(zoneableHeader.getSecurityZoneOid());
    }
}
