package com.l7tech.console.util;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.folder.HasFolderOid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EntityNameResolverTest {
    private static final long OID = 1234L;
    private static final String NAME = "test";
    private EntityNameResolver resolver;
    @Mock
    private ServiceAdmin serviceAdmin;
    @Mock
    private PolicyAdmin policyAdmin;
    @Mock
    private TrustedCertAdmin trustedCertAdmin;
    @Mock
    private ResourceAdmin resourceAdmin;
    @Mock
    private FolderAdmin folderAdmin;

    @Before
    public void setup() {
        resolver = new EntityNameResolver(serviceAdmin, policyAdmin, trustedCertAdmin, resourceAdmin, folderAdmin);
    }

    @Test
    public void getNameForHeaderUsesNameOnHeader() throws Exception {
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader(null, null, "test", null)));
    }

    @Test
    public void getNameForServiceAlias() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(NAME);
        service.setRoutingUri("/routingUri");
        when(serviceAdmin.findByAlias(OID)).thenReturn(service);
        assertEquals("test alias[/routingUri] (/test alias)", resolver.getNameForHeader(new EntityHeader(OID, EntityType.SERVICE_ALIAS, null, null)));
    }

    @Test
    public void getNameForServiceAliasNullRoutingUri() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(NAME);
        service.setRoutingUri(null);
        when(serviceAdmin.findByAlias(OID)).thenReturn(service);
        assertEquals("test alias (/test alias)", resolver.getNameForHeader(new EntityHeader(OID, EntityType.SERVICE_ALIAS, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForServiceAliasNotFound() throws Exception {
        when(serviceAdmin.findByAlias(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(OID, EntityType.SERVICE_ALIAS, null, null));
    }

    @Test
    public void getNameForPolicyAlias() throws Exception {
        when(policyAdmin.findByAlias(OID)).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, NAME, "xml", false));
        assertEquals("test alias (/test alias)", resolver.getNameForHeader(new EntityHeader(OID, EntityType.POLICY_ALIAS, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForPolicyAliasNotFound() throws Exception {
        when(policyAdmin.findByAlias(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(OID, EntityType.POLICY_ALIAS, null, null));
    }

    @Test
    public void getNameForKeyMetadata() throws Exception {
        when(trustedCertAdmin.findKeyMetadata(OID)).thenReturn(new SsgKeyMetadata(1L, NAME, null));
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader(OID, EntityType.SSG_KEY_METADATA, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForKeyMetadataNotFound() throws Exception {
        when(trustedCertAdmin.findKeyMetadata(anyLong())).thenReturn(null);
        assertEquals(NAME, resolver.getNameForHeader(new EntityHeader(OID, EntityType.SSG_KEY_METADATA, null, null)));
    }

    @Test
    public void getNameForResourceEntry() throws Exception {
        final String uri = "http://localhost:8080";
        final ResourceEntry resourceEntry = new ResourceEntry();
        resourceEntry.setUri(uri);
        when(resourceAdmin.findResourceEntryByPrimaryKey(OID)).thenReturn(resourceEntry);
        assertEquals(uri, resolver.getNameForHeader(new EntityHeader(OID, EntityType.RESOURCE_ENTRY, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForResourceEntryNotFound() throws Exception {
        when(resourceAdmin.findResourceEntryByPrimaryKey(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(OID, EntityType.RESOURCE_ENTRY, null, null));
    }

    @Test
    public void getNameForHttpConfiguration() throws Exception {
        final HttpConfiguration config = new HttpConfiguration();
        config.setProtocol(HttpConfiguration.Protocol.HTTP);
        config.setHost("localhost");
        config.setPort(8080);
        when(resourceAdmin.findHttpConfigurationByPrimaryKey(OID)).thenReturn(config);
        assertEquals("HTTP localhost 8080", resolver.getNameForHeader(new EntityHeader(OID, EntityType.HTTP_CONFIGURATION, null, null)));
    }

    @Test(expected = FindException.class)
    public void getNameForHttpConfigurationNotFound() throws Exception {
        when(resourceAdmin.findHttpConfigurationByPrimaryKey(anyLong())).thenReturn(null);
        resolver.getNameForHeader(new EntityHeader(OID, EntityType.HTTP_CONFIGURATION, null, null));
    }

    @Test
    public void geNameForHeaderUnsupportedEntityType() throws Exception {
        assertTrue(resolver.getNameForHeader(new EntityHeader(OID, EntityType.ANY, null, null)).isEmpty());
    }

    /**
     * If the header has the entity's oid as its name - go get more info for the name.
     */
    @Test
    public void getNameForHeaderWithOidInName() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(NAME);
        when(serviceAdmin.findByAlias(OID)).thenReturn(service);
        assertEquals("test alias (/test alias)", resolver.getNameForHeader(new EntityHeader(OID, EntityType.SERVICE_ALIAS, String.valueOf(OID), null)));
    }

    @Test
    public void getNameForHasFolderOidHeader() throws Exception {
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))));
        final HasFolderOidStub header = new HasFolderOidStub(OID);
        header.setName(NAME);
        final String name = resolver.getNameForHeader(header);
        assertEquals("test (/folder1/folder2/test)", name);
    }

    @Test
    public void getNameForHasFolderOidHeaderNoName() throws Exception {
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))));
        final HasFolderOidStub header = new HasFolderOidStub(OID);
        header.setName(null);
        final String name = resolver.getNameForHeader(header);
        assertEquals(" (/folder1/folder2/)", name);
    }

    @Test
    public void getPathNoFolder() throws Exception {
        assertEquals("/", resolver.getPath(new HasFolderStub(null)));
    }

    /**
     * Don't show the root node folder
     */
    @Test
    public void getPathSingleFolder() throws Exception {
        final String path = resolver.getPath(new HasFolderStub(new Folder("Root Node", null)));
        assertEquals("/", path);
    }

    @Test
    public void getPathLessThanMaxPathLength() throws Exception {
        final String path = resolver.getPath(new HasFolderStub(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null)))));
        assertEquals("/folder1/folder2/", path);
    }

    @Test
    public void getPathMaxPathLength() throws Exception {
        final String path = resolver.getPath(new HasFolderStub(new Folder("folder3", new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))))));
        assertEquals("/folder1/folder2/folder3/", path);
    }

    @Test
    public void getPathGreaterThanMaxPathLength() throws Exception {
        final String path = resolver.getPath(new HasFolderStub(new Folder("folder4", new Folder("folder3", new Folder("folder2", new Folder("folder1", new Folder("Root Node", null)))))));
        assertEquals("/folder1/.../folder4/", path);
    }

    @Test
    public void getPathFromFolderOid() throws Exception {
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(new Folder("folder2", new Folder("folder1", new Folder("Root Node", null))));
        final String path = resolver.getPath(new HasFolderOidStub(OID));
        assertEquals("/folder1/folder2/", path);
    }

    @Test
    public void getPathFromNullFolderOid() throws Exception {
        final String path = resolver.getPath(new HasFolderOidStub(null));
        assertEquals("/", path);
        verify(folderAdmin, never()).findByPrimaryKey(anyLong());
    }

    @Test
    public void getPathFromFolderOidDoesNotExist() throws Exception {
        when(folderAdmin.findByPrimaryKey(OID)).thenReturn(null);
        assertEquals("/", resolver.getPath(new HasFolderOidStub(OID)));
    }

    @Test(expected = FindException.class)
    public void getPathFromFolderOidFindException() throws Exception {
        when(folderAdmin.findByPrimaryKey(OID)).thenThrow(new FindException("mocking exception"));
        resolver.getPath(new HasFolderOidStub(OID));
    }

    @Test
    public void getNameForServiceHeaderIncludesRoutingUri() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(NAME);
        service.setRoutingUri("/routingUri");
        assertEquals("test[/routingUri] (/test)", resolver.getNameForHeader(new ServiceHeader(service)));
    }

    @Test
    public void getNameForHeaderNullTypeAndName() throws Exception {
        assertTrue(resolver.getNameForHeader(new EntityHeader(null, null, null, null)).isEmpty());
    }

    @Test
    public void getNameWithoutPath() throws Exception {
        final PublishedService service = new PublishedService();
        service.setName(NAME);
        service.setRoutingUri("/routingUri");
        assertEquals("test[/routingUri]", resolver.getNameForHeader(new ServiceHeader(service), false));
    }

    private class HasFolderStub implements HasFolder {
        private Folder folder;

        private HasFolderStub(final Folder folder) {
            this.folder = folder;
        }

        @Override
        public Folder getFolder() {
            return folder;
        }

        @Override
        public void setFolder(final Folder folder) {
            this.folder = folder;
        }
    }

    private class HasFolderOidStub extends EntityHeader implements HasFolderOid {
        private Long folderOid;

        private HasFolderOidStub(final Long folderOid) {
            this.folderOid = folderOid;
        }

        @Override
        public Long getFolderOid() {
            return folderOid;
        }

        @Override
        public void setFolderOid(final Long folderOid) {
            this.folderOid = folderOid;
        }
    }
}
