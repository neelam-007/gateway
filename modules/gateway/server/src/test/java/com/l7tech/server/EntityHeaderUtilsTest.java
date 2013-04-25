package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.test.BugId;
import org.junit.Test;

import static org.junit.Assert.*;

public class EntityHeaderUtilsTest {
    private static final String GUID = "abc123";
    private static final String NAME = "test";
    private static final String DESCRIPTION = "description";
    private static final Integer VERSION = 1;

    @BugId("EM-914")
    @Test
    public void fromEntityEncapsulatedAssertionConfig() {
        final GuidEntityHeader header = (GuidEntityHeader) EntityHeaderUtils.fromEntity(createEncapsulatedAssertionConfig(GUID, NAME, VERSION));
        assertEquals(GUID, header.getGuid());
        assertEquals(GUID, header.getStrId());
        assertEquals(EntityType.ENCAPSULATED_ASSERTION, header.getType());
        assertEquals(NAME, header.getName());
        assertEquals(VERSION, header.getVersion());
        assertNull(header.getDescription());
    }

    @Test
    public void fromEntityEncapsulatedAssertionConfigNullValues() {
        final GuidEntityHeader header = (GuidEntityHeader) EntityHeaderUtils.fromEntity(createEncapsulatedAssertionConfig(null, null, VERSION));
        assertNull(header.getGuid());
        assertNull(header.getStrId());
        assertNull(header.getName());
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
        zone.setOid(1234L);
        folder.setSecurityZone(zone);
        final FolderHeader header = (FolderHeader) EntityHeaderUtils.fromEntity(folder);
        assertEquals(new Long(1234), header.getSecurityZoneOid());
    }

    @Test
    public void fromEntityFolderSetsNullSecurityZoneOid() {
        final Folder folder = new Folder("testFolder", null);
        folder.setSecurityZone(null);
        final FolderHeader header = (FolderHeader) EntityHeaderUtils.fromEntity(folder);
        assertNull(header.getSecurityZoneOid());
    }

    private EncapsulatedAssertionConfig createEncapsulatedAssertionConfig(final String guid, final String name, final Integer version) {
        final EncapsulatedAssertionConfig encassConfig = new EncapsulatedAssertionConfig();
        encassConfig.setGuid(guid);
        encassConfig.setName(name);
        encassConfig.setVersion(version);
        return encassConfig;
    }

    private GuidEntityHeader createGuidEntityHeader(final String guid, final EntityType entityType, final String name, final String description, final Integer version) {
        final GuidEntityHeader guidHeader = new GuidEntityHeader(guid, entityType, name, description, version);
        guidHeader.setGuid(guid);
        return guidHeader;
    }
}
