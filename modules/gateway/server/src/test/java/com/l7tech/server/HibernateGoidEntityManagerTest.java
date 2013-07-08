package com.l7tech.server;

import com.l7tech.objectmodel.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HibernateGoidEntityManagerTest {
    private static final Goid GOID = new Goid(0,1234L);
    private static final Integer VER = 1;
    private static final Long ZONE_OID = 1111L;
    private HibernateGoidEntityManager<GoidEntity, EntityHeader> genericHeaderManager;
    private SecurityZone zone;

    @Before
    public void setup() {
        genericHeaderManager = new TestableHibernateEntityManager();
        zone = new SecurityZone();
        zone.setOid(ZONE_OID);
    }

    @Test
    public void newHeaderEntityNotZoneable() {
        final EntityHeader header = genericHeaderManager.newHeader(new StubGoidEntity(GOID, VER));
        assertEquals(GOID, header.getGoid());
        assertEquals(VER, header.getVersion());
        assertTrue(header.getDescription().isEmpty());
        assertEquals(EntityType.ANY, header.getType());
        assertFalse(header instanceof ZoneableEntityHeader);
    }

    @Test
    public void newHeaderEntityZoneable() {
        final ZoneableEntityHeader header = (ZoneableEntityHeader) genericHeaderManager.newHeader(new StubZoneableEntity(GOID, VER, zone));
        assertEquals(ZONE_OID, header.getSecurityZoneOid());
        assertEquals(GOID, header.getGoid());
        assertEquals(VER, header.getVersion());
        assertTrue(header.getDescription().isEmpty());
        assertEquals(EntityType.ANY, header.getType());
    }

    @Test
    public void newHeaderEntityZoneableNullZone() {
        final ZoneableEntityHeader header = (ZoneableEntityHeader) genericHeaderManager.newHeader(new StubZoneableEntity(GOID, VER, null));
        assertNull(header.getSecurityZoneOid());
    }

    private class TestableHibernateEntityManager extends HibernateGoidEntityManager<GoidEntity, EntityHeader> {
        @Override
        public Class<? extends Entity> getImpClass() {
            return StubGoidEntity.class;
        }

        @Override
        public String getTableName() {
            return "stub";
        }
    }

    private class StubGoidEntity implements GoidEntity {
        private Goid goid;
        private int version;

        private StubGoidEntity() {
        }

        private StubGoidEntity(final Goid goid, final int version) {
            this.goid = goid;
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
        public Goid getGoid() {
            return goid;
        }

        @Override
        public void setGoid(Goid goid) {
            this.goid = goid;
        }

        @Override
        public String getId() {
            return String.valueOf(goid);
        }
    }

    private class StubZoneableEntity extends StubGoidEntity implements ZoneableEntity {
        private SecurityZone zone;

        private StubZoneableEntity(final Goid goid, final int version, final SecurityZone zone) {
            super(goid, version);
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
