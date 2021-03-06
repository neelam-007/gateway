package com.l7tech.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.imp.*;
import com.l7tech.test.BugId;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.GoidUpgradeMapperTestUtil;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class EntityHeaderUtilsTest {
    private static final Goid GOID = new Goid(new Random().nextLong(), new Random().nextLong());
    private static final Goid ZONE_GOID = new Goid(0,1111L);
    private static final String GUID = "abc123";
    private static final String NAME = "test";
    private static final String DESCRIPTION = "description";
    private static final Integer VERSION = 1;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setGoid(ZONE_GOID);
    }

    @BugId("EM-914")
    @Test
    public void fromEntityEncapsulatedAssertionConfig() {
        final ZoneableGuidEntityHeader header = (ZoneableGuidEntityHeader) EntityHeaderUtils.fromEntity(createEncapsulatedAssertionConfig(GOID, GUID, NAME, VERSION, zone));
        assertEquals(GUID, header.getGuid());
        assertEquals(GOID, header.getGoid());
        assertEquals(EntityType.ENCAPSULATED_ASSERTION, header.getType());
        assertEquals(NAME, header.getName());
        assertEquals(VERSION, header.getVersion());
        assertNull(header.getDescription());
        assertEquals(ZONE_GOID, header.getSecurityZoneId());
    }

    @Test
    public void fromEntityEncapsulatedAssertionConfigNullValues() {
        final ZoneableGuidEntityHeader header = (ZoneableGuidEntityHeader) EntityHeaderUtils.fromEntity(createEncapsulatedAssertionConfig(GOID, null, null, VERSION, null));
        assertNull(header.getGuid());
        assertNull(header.getName());
        assertEquals(GOID, header.getGoid());
        assertNull(header.getSecurityZoneId());
    }

    @BugId("EM-914")
    @Test
    public void toExternalGuidEntityHeader() {
        final ExternalEntityHeader externalHeader = EntityHeaderUtils.toExternal(createGuidEntityHeader(GUID, EntityType.ENCAPSULATED_ASSERTION, NAME, DESCRIPTION, VERSION));
        assertEquals(GUID, externalHeader.getStrId());
        assertEquals(GUID, externalHeader.getExternalId());
        assertEquals(EntityType.ENCAPSULATED_ASSERTION, externalHeader.getType());
        assertEquals(NAME, externalHeader.getName());
        assertEquals(DESCRIPTION, externalHeader.getDescription());
        assertEquals(VERSION, externalHeader.getVersion());
    }

    @Test
    public void toExternalGuidEntityHeaderNullValues() {
        final ExternalEntityHeader externalHeader = EntityHeaderUtils.toExternal(createGuidEntityHeader(null, null, null, null, null));
        assertNull(externalHeader.getStrId());
        assertNull(externalHeader.getExternalId());
        assertNull(externalHeader.getType());
        assertNull(externalHeader.getName());
        assertNull(externalHeader.getDescription());
        assertNull(externalHeader.getVersion());
    }

    @BugId("EM-914")
    @Test
    public void fromExternalEncapsulatedAssertion() {
        final GuidEntityHeader guidHeader = (GuidEntityHeader) EntityHeaderUtils.fromExternal(new ExternalEntityHeader(GUID, EntityType.ENCAPSULATED_ASSERTION, GUID, NAME, DESCRIPTION, VERSION));
        assertEquals(GUID, guidHeader.getGuid());
        assertEquals(GUID, guidHeader.getStrId());
        assertEquals(EntityType.ENCAPSULATED_ASSERTION, guidHeader.getType());
        assertEquals(NAME, guidHeader.getName());
        assertEquals(DESCRIPTION, guidHeader.getDescription());
        assertEquals(VERSION, guidHeader.getVersion());
    }

    @Test
    public void fromExternalEncapsulatedAssertionNullValues() {
        final GuidEntityHeader guidHeader = (GuidEntityHeader) EntityHeaderUtils.fromExternal(new ExternalEntityHeader(null, EntityType.ENCAPSULATED_ASSERTION, null, null, null, null));
        assertNull(guidHeader.getGuid());
        assertNull(guidHeader.getStrId());
        assertEquals(EntityType.ENCAPSULATED_ASSERTION, guidHeader.getType());
        assertNull(guidHeader.getName());
        assertNull(guidHeader.getDescription());
        assertNull(guidHeader.getVersion());
    }

    @Test
    public void fromEntityFolderSetsSecurityZoneOid() {
        final Folder folder = new Folder("testFolder", null);
        final SecurityZone zone = new SecurityZone();
        zone.setGoid(new Goid(0,1234L));
        folder.setSecurityZone(zone);
        final FolderHeader header = (FolderHeader) EntityHeaderUtils.fromEntity(folder);
        assertEquals(new Goid(0,1234L), header.getSecurityZoneId());
    }

    @Test
    public void fromEntityFolderSetsNullSecurityZoneOid() {
        final Folder folder = new Folder("testFolder", null);
        folder.setSecurityZone(null);
        final FolderHeader header = (FolderHeader) EntityHeaderUtils.fromEntity(folder);
        assertNull(header.getSecurityZoneId());
    }

    @Test
    public void fromEntityJmsEndpointSetsSecurityZoneOid() {
        final JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.setSecurityZone(zone);
        final JmsEndpointHeader header = (JmsEndpointHeader) EntityHeaderUtils.fromEntity(endpoint);
        assertEquals(ZONE_GOID, header.getSecurityZoneId());
    }

    @Test
    public void fromEntityJmsEndpointSetsNullSecurityZoneOid() {
        final JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.setSecurityZone(null);
        final JmsEndpointHeader header = (JmsEndpointHeader) EntityHeaderUtils.fromEntity(endpoint);
        assertNull(header.getSecurityZoneId());
    }

    @BugId("SSG-6919")
    @Test
    public void fromEntityJmsEndpointSetsConnectionGoid() {
        final JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.setMessageSource(true);
        final Goid connectionGoid = new Goid(0, 1);
        endpoint.setConnectionGoid(connectionGoid);
        final JmsEndpointHeader header = (JmsEndpointHeader) EntityHeaderUtils.fromEntity(endpoint);
        assertEquals(connectionGoid, header.getConnectionGoid());
        assertTrue(header.isIncoming());
    }

    @Test
    public void fromEntityZoneablePersistentEntitySetsSecurityZoneOid() {
        final StubZoneablePersistentEntity entity = new StubZoneablePersistentEntity(GOID, VERSION, zone);
        final ZoneableEntityHeader header = (ZoneableEntityHeader) EntityHeaderUtils.fromEntity(entity);
        assertEquals(ZONE_GOID, header.getSecurityZoneId());
        assertEquals(GOID, header.getGoid());
        assertNull(header.getName());
        assertNull(header.getDescription());
        // unknown entity type b/c it's a stub class
        assertEquals(EntityType.ANY, header.getType());
    }

    @Test
    public void fromEntityZoneablePersistentEntitySetsNullSecurityZoneOid() {
        final StubZoneablePersistentEntity entity = new StubZoneablePersistentEntity(GOID, VERSION, null);
        final ZoneableEntityHeader header = (ZoneableEntityHeader) EntityHeaderUtils.fromEntity(entity);
        assertNull(header.getSecurityZoneId());
    }

    @Test
    public void fromEntityZoneableNamedEntitySetsSecurityZoneOid() {
        final StubZoneableNamedEntity entity = new StubZoneableNamedEntity(GOID, VERSION, NAME, zone);
        final ZoneableEntityHeader header = (ZoneableEntityHeader) EntityHeaderUtils.fromEntity(entity);
        assertEquals(ZONE_GOID, header.getSecurityZoneId());
        assertEquals(GOID, header.getGoid());
        assertEquals(NAME, header.getName());
        assertNull(header.getDescription());
        assertEquals(EntityType.ANY, header.getType());
    }

    @Test
    public void fromEntityZoneableNamedEntitySetsNullSecurityZoneOid() {
        final StubZoneableNamedEntity entity = new StubZoneableNamedEntity(GOID, VERSION, NAME, null);
        final ZoneableEntityHeader header = (ZoneableEntityHeader) EntityHeaderUtils.fromEntity(entity);
        assertNull(header.getSecurityZoneId());
    }

    @Test
    public void fromEntityZoneableNonPersistentEntitySetsSecurityZoneOid() {
        final StubZoneableNonPersistentEntity entity = new StubZoneableNonPersistentEntity(String.valueOf(GOID), zone);
        final ZoneableEntityHeader header = (ZoneableEntityHeader) EntityHeaderUtils.fromEntity(entity);
        assertEquals(ZONE_GOID, header.getSecurityZoneId());
        assertEquals(GOID, header.getGoid());
    }

    @Test
    public void fromEntityZoneableNonPersistentEntitySetsNullSecurityZoneOid() {
        final StubZoneableNonPersistentEntity entity = new StubZoneableNonPersistentEntity(String.valueOf(GOID), null);
        final ZoneableEntityHeader header = (ZoneableEntityHeader) EntityHeaderUtils.fromEntity(entity);
        assertNull(header.getSecurityZoneId());
    }

    @Test
    public void fromEntityPersistentEntity() {
        final StubPersistentEntity entity = new StubPersistentEntity(GOID, VERSION);
        final EntityHeader header = EntityHeaderUtils.fromEntity(entity);
        assertEquals(GOID, header.getGoid());
        assertEquals(VERSION, header.getVersion());
        assertFalse(header instanceof ZoneableEntityHeader);
    }

    @Test
    public void fromEntityNamedEntity() {
        final StubNamedEntity entity = new StubNamedEntity(GOID, VERSION, NAME);
        final EntityHeader header = EntityHeaderUtils.fromEntity(entity);
        assertEquals(GOID, header.getGoid());
        assertEquals(VERSION, header.getVersion());
        assertEquals(NAME, header.getName());
        assertFalse(header instanceof ZoneableEntityHeader);
    }

    @Test
    public void fromEntityGoidEntity() {
        final PersistentEntityImp entity = new PersistentEntityImp(){{setGoid(GOID); setVersion(VERSION);}};
        final EntityHeader header = EntityHeaderUtils.fromEntity(entity);
        assertEquals(GOID, header.getGoid());
        assertEquals(VERSION, header.getVersion());
        assertFalse(header instanceof ZoneableEntityHeader);
    }

    @Test
    public void fromEntityGoidNamedEntity() {
        final NamedEntityImp entity = new NamedEntityImp(){{setGoid(GOID); setVersion(VERSION); setName(NAME);}};
        final EntityHeader header = EntityHeaderUtils.fromEntity(entity);
        assertEquals(GOID, header.getGoid());
        assertEquals(VERSION, header.getVersion());
        assertEquals(NAME, header.getName());
        assertFalse(header instanceof ZoneableEntityHeader);
    }

    @Test
    public void fromExternalOldOidIdentityEntity(){
        final IdentityHeader idHeader = (IdentityHeader) EntityHeaderUtils.fromExternal(new ExternalEntityHeader("-2:1", EntityType.USER, null , NAME, DESCRIPTION, VERSION));
        assertEquals(EntityType.USER, idHeader.getType());
        assertEquals(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, idHeader.getProviderGoid());
        assertEquals(GoidUpgradeMapper.mapId("internal_user","1").toString(), idHeader.getStrId());

        final IdentityHeader idGoidHeader = (IdentityHeader) EntityHeaderUtils.fromExternal(new ExternalEntityHeader(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString()+":"+GOID.toString(), EntityType.USER, null , NAME, DESCRIPTION, VERSION));
        assertEquals(EntityType.USER, idGoidHeader.getType());
        assertEquals(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, idGoidHeader.getProviderGoid());
        assertEquals(GOID.toString(), idGoidHeader.getStrId());

        final IdentityHeader fedIdHeader = (IdentityHeader) EntityHeaderUtils.fromExternal(new ExternalEntityHeader("123:123", EntityType.USER, null , NAME, DESCRIPTION, VERSION));
        assertEquals(EntityType.USER, fedIdHeader.getType());
        assertEquals(GoidUpgradeMapper.mapId(EntityType.ID_PROVIDER_CONFIG,"123"), fedIdHeader.getProviderGoid());
        assertEquals(GoidUpgradeMapper.mapId("fed_user","123").toString(), fedIdHeader.getStrId());

        final IdentityHeader fedGoidIdHeader = (IdentityHeader) EntityHeaderUtils.fromExternal(new ExternalEntityHeader(GOID.toString()+":"+GOID.toString(), EntityType.USER, null , NAME, DESCRIPTION, VERSION));
        assertEquals(EntityType.USER, fedGoidIdHeader.getType());
        assertEquals(GOID, fedGoidIdHeader.getProviderGoid());
        assertEquals(GOID.toString(), fedGoidIdHeader.getStrId());

        final IdentityHeader LDAPIdHeader = (IdentityHeader) EntityHeaderUtils.fromExternal(new ExternalEntityHeader("123:cn=asdf", EntityType.USER, null , NAME, DESCRIPTION, VERSION));
        assertEquals(EntityType.USER, LDAPIdHeader.getType());
        assertEquals(GoidUpgradeMapper.mapId(EntityType.ID_PROVIDER_CONFIG,"123"), LDAPIdHeader.getProviderGoid());
        assertEquals("cn=asdf", LDAPIdHeader.getStrId());

    }

    @BugId("EM-994")
    @Test
    public void clusterPropertyExternalHeaderTest() {
        ClusterProperty cp = new ClusterProperty("MyProperty", "MyValue");
        cp.setGoid(new Goid(123, 456));

        EntityHeader cpHeader = EntityHeaderUtils.fromEntity(cp);

        ExternalEntityHeader cpExternalHeader = EntityHeaderUtils.toExternal(cpHeader);

        EntityHeader cpRecreatedHeader = EntityHeaderUtils.fromExternal(cpExternalHeader);

        Assert.assertEquals(cpHeader.getName(), cpRecreatedHeader.getName());
        Assert.assertEquals(cpHeader.getGoid(), cpRecreatedHeader.getGoid());
        Assert.assertEquals(cpHeader.getDescription(), cpRecreatedHeader.getDescription());
        Assert.assertEquals(cpHeader.getType(), cpRecreatedHeader.getType());
        Assert.assertEquals(cpHeader.getVersion(), cpRecreatedHeader.getVersion());
    }

    private EncapsulatedAssertionConfig createEncapsulatedAssertionConfig(final Goid goid, final String guid, final String name, final Integer version, final SecurityZone zone) {
        final EncapsulatedAssertionConfig encassConfig = new EncapsulatedAssertionConfig();
        encassConfig.setGoid(goid);
        encassConfig.setGuid(guid);
        encassConfig.setName(name);
        encassConfig.setVersion(version);
        encassConfig.setSecurityZone(zone);
        return encassConfig;
    }

    private GuidEntityHeader createGuidEntityHeader(final String guid, final EntityType entityType, final String name, final String description, final Integer version) {
        final GuidEntityHeader guidHeader = new GuidEntityHeader(guid, entityType, name, description, version);
        guidHeader.setGuid(guid);
        return guidHeader;
    }

    private class StubPersistentEntity extends PersistentEntityImp {
        private StubPersistentEntity(final Goid goid, final int version) {
            setGoid(goid);
            setVersion(version);
        }
    }

    private class StubNamedEntity extends NamedEntityImp {
        private StubNamedEntity(final Goid goid, final int version, final String name) {
            setGoid(goid);
            setVersion(version);
            setName(name);
        }
    }

    private class StubNonPersistentEntity implements Entity {
        private String id;

        private StubNonPersistentEntity(final String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return null;
        }
    }

    private class StubZoneablePersistentEntity extends ZoneableEntityImp {
        private StubZoneablePersistentEntity(final Goid goid, final int version, final SecurityZone zone) {
            setGoid(goid);
            setVersion(version);
            setSecurityZone(zone);
        }
    }

    private class StubZoneableNamedEntity extends ZoneableNamedEntityImp {
        private StubZoneableNamedEntity(final Goid goid, final int version, final String name, final SecurityZone zone) {
            setGoid(goid);
            setVersion(version);
            setName(name);
            setSecurityZone(zone);
        }
    }

    private class StubZoneableNonPersistentEntity implements Entity, ZoneableEntity {
        private String id;
        private SecurityZone zone;

        private StubZoneableNonPersistentEntity(final String id, final SecurityZone zone) {
            this.id = id;
            this.zone = zone;
        }

        @Override
        public SecurityZone getSecurityZone() {
            return zone;
        }

        @Override
        public void setSecurityZone(final SecurityZone securityZone) {
            this.zone = securityZone;
        }

        @Override
        public String getId() {
            return id;
        }
    }
}
