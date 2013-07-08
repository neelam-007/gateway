package com.l7tech.objectmodel;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class ZoneableEntityHeaderTest {
    private static final long OID = 1234L;
    private static final Goid GOID = new Goid(new Random().nextLong(),new Random().nextLong());
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

    @Test
    public void copyConstructorGoid() {
        final EntityHeader entityHeader = new EntityHeader(GOID, EntityType.POLICY, NAME, DESC, VER);
        final ZoneableEntityHeader zoneableHeader = new ZoneableEntityHeader(entityHeader);
        assertEquals(GOID, zoneableHeader.getGoid());
        assertEquals(NAME, zoneableHeader.getName());
        assertEquals(DESC, zoneableHeader.getDescription());
        assertEquals(VER, zoneableHeader.getVersion());
        assertEquals(EntityType.POLICY, zoneableHeader.getType());
        assertNull(zoneableHeader.getSecurityZoneOid());
    }
}
