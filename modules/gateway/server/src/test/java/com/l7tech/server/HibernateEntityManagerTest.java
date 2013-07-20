package com.l7tech.server;

import com.l7tech.objectmodel.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HibernateEntityManagerTest {
    private static final long OID = 1234L;
    private static final Integer VER = 1;
    private static final Goid ZONE_GOID = new Goid(0,1111L);
    private HibernateEntityManager<PersistentEntity, EntityHeader> genericHeaderManager;
    private SecurityZone zone;

    @Before
    public void setup() {
        genericHeaderManager = new TestableHibernateEntityManager();
        zone = new SecurityZone();
        zone.setGoid(ZONE_GOID);
    }

    @Test
    public void newHeaderEntityNotZoneable() {
        final EntityHeader header = genericHeaderManager.newHeader(new StubPersistentEntity(OID, VER));
        assertEquals(OID, header.getOid());
        assertEquals(VER, header.getVersion());
        assertTrue(header.getDescription().isEmpty());
        assertEquals(EntityType.ANY, header.getType());
        assertFalse(header instanceof ZoneableEntityHeader);
    }

    @Test
    public void newHeaderEntityZoneable() {
        final ZoneableEntityHeader header = (ZoneableEntityHeader) genericHeaderManager.newHeader(new StubZoneableEntity(OID, VER, zone));
        assertEquals(ZONE_GOID, header.getSecurityZoneGoid());
        assertEquals(OID, header.getOid());
        assertEquals(VER, header.getVersion());
        assertTrue(header.getDescription().isEmpty());
        assertEquals(EntityType.ANY, header.getType());
    }

    @Test
    public void newHeaderEntityZoneableNullZone() {
        final ZoneableEntityHeader header = (ZoneableEntityHeader) genericHeaderManager.newHeader(new StubZoneableEntity(OID, VER, null));
        assertNull(header.getSecurityZoneGoid());
    }

    private class TestableHibernateEntityManager extends HibernateEntityManager<PersistentEntity, EntityHeader> {
        @Override
        public Class<? extends Entity> getImpClass() {
            return StubPersistentEntity.class;
        }

        @Override
        public String getTableName() {
            return "stub";
        }
    }

    private class StubPersistentEntity implements PersistentEntity {
        private long oid;
        private int version;

        private StubPersistentEntity() {
        }

        private StubPersistentEntity(final long oid, final int version) {
            this.oid = oid;
            this.version = version;
        }

        @Override
        public int getVersion() {
            return version;
        }

        @Override
        public void setVersion(int version) {
            this.version = version;
        }

        @Override
        public long getOid() {
            return oid;
        }

        @Override
        public void setOid(long oid) {
            this.oid = oid;
        }

        @Override
        public String getId() {
            return String.valueOf(oid);
        }
    }

    private class StubZoneableEntity extends StubPersistentEntity implements ZoneableEntity {
        private SecurityZone zone;

        private StubZoneableEntity(final long oid, final int version, final SecurityZone zone) {
            super(oid, version);
            this.zone = zone;
        }

        @Override
        public SecurityZone getSecurityZone() {
            return zone;
        }

        @Override
        public void setSecurityZone(SecurityZone securityZone) {
            this.zone = zone;
        }
    }
}
